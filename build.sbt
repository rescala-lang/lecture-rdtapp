lazy val chat = project.in(file("."))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    scalaVersion := "3.5.0",
    libraryDependencies ++= Seq(
      "de.tu-darmstadt.stg" %%% "reactives"   % "0.36.0",
      "de.tu-darmstadt.stg" %%% "rdts"        % "0.36.0",
      "org.scala-js"        %%% "scalajs-dom" % "2.8.0",
      "com.lihaoyi"         %%% "scalatags"   % "0.13.1",
      // Below imports are required for the restoration and distribution.
      "com.github.plokhotnyuk.jsoniter-scala" %%% "jsoniter-scala-core"   % "2.30.9",
      "com.github.plokhotnyuk.jsoniter-scala"  %% "jsoniter-scala-macros" % "2.30.9",
    ),
  )
