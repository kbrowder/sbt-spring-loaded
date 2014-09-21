/**
 * Copyright 2014 Kevin Browder
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.browder

import collection.JavaConversions._
import scala.collection._
import java.util.concurrent.{ ConcurrentMap, ConcurrentHashMap }

import sbt._
import sbt.Keys._
import Def.Initialize

import org.springsource.loaded.SpringLoaded

object SbtSpringLoaded extends AutoPlugin {
  object autoImport {
    lazy val reStart = inputKey[ForkedProc]("Start/Restart the mainClass in a separate JVM with the spring-reloaded agent")
    lazy val reStop = inputKey[Unit]("Stop a JVM forked with reStart")
    lazy val reList = inputKey[Unit]("Show projects with running JVMs created with reStart")

    lazy val forkedProcs = settingKey[mutable.Map[ProjectRef, ForkedProc]]("Mapping of projects to there forked process")
  }
  import autoImport._

  override def trigger = allRequirements
  override def requires = plugins.JvmPlugin

  override def projectSettings = Seq(
    mainClass in reStart <<= mainClass in run in Compile,
    fullClasspath in reStart <<= fullClasspath in Runtime,
    forkedProcs in reStart := mutable.HashMap.empty[ProjectRef, ForkedProc],
    reStart <<= reStartTask(fullClasspath in reStart, mainClass in reStart, forkedProcs in reStart),
    reStop <<= reStopTask(forkedProcs in reStart),
    reList <<= reListTask(forkedProcs in reStart))

  def reStartTask(classpath: Initialize[Task[Classpath]],
                  mainClassTask: Initialize[Task[Option[String]]],
                  forkedProcs: SettingKey[mutable.Map[ProjectRef, ForkedProc]]): Initialize[InputTask[ForkedProc]] = {
    import Def.parserToInput
    val parser = Def.spaceDelimited()

    Def.inputTask {

      lazy val springLoadedFile = new File(
        classOf[SpringLoaded].
          getProtectionDomain().
          getCodeSource().
          getLocation().
          toURI().
          getPath())

      lazy val springLoadedPath = springLoadedFile.getAbsolutePath()

      val log = streams.value.log
      val forkOptions = ForkOptions(
        runJVMOptions = javaOptions.value ++ Seq(s"-javaagent:$springLoadedPath", "-noverify"),
        connectInput = false)

      val options: Seq[String] = parser.parsed
      val classpathPart: Seq[String] = "-classpath" :: Path.makeString(classpath.value.map(_.data)) :: Nil
      val mainClass: String = mainClassTask.value getOrElse error("No main class found.")
      val scalaOptions = (classpathPart :+ mainClass) ++ options.toList

      log.warn(s"running $mainClass " + options.mkString)
      val process = Fork.java.fork(forkOptions, scalaOptions)

      val projectRef = thisProjectRef.value
      for (oldProc <- forkedProcs.value.get(projectRef)) oldProc.destroy()

      val forkedProc = ForkedProc(thisProjectRef.value, log)(process)

      forkedProcs.value += (projectRef -> forkedProc)

      forkedProc
    }
  }

  def reStopTask(forkedProcs: SettingKey[mutable.Map[ProjectRef, ForkedProc]]) = Def.inputTask {
    val log = streams.value.log
    val projectRef = thisProjectRef.value
    for (oldProc <- forkedProcs.value.get(projectRef)) {
      log.warn(s"Killing old proc from $projectRef")
      oldProc.destroy()
    }
  }
  
  def reListTask(forkedProcs: SettingKey[mutable.Map[ProjectRef, ForkedProc]]) = Def.inputTask {
    println("\nThe following projects have reStart jobs running:")
    for ((projectRef, forkedProc) <- forkedProcs.value if forkedProc.running) {
      println("  * " + projectRef.project)
    }
    println()
  }

}