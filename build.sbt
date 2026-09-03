lazy val rdtapp = project.in(file("."))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    scalaVersion := "3.9.0",
    libraryDependencies ++= Seq(
      "de.tu-darmstadt.stg"                   %% "reactives"             % "0.38.0",
      "de.tu-darmstadt.stg"                   %% "rdts"                  % "0.38.0",
      "de.tu-darmstadt.stg"                           %% "channels"           % "0.38.0",
      "org.scala-js"                          %% "scalajs-dom"           % "2.8.0",
      "com.lihaoyi"                           %% "scalatags"             % "0.13.1",
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core"   % "2.40.1",
      "com.github.plokhotnyuk.jsoniter-scala"  %% "jsoniter-scala-macros" % "2.40.1",
      "com.lihaoyi"                          %% "pprint"                % "0.9.6",
      "org.scalameta"                         %% "munit"                 % "1.3.6" % Test,
    ),
  )
