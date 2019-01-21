name := "monix-kafka-consumer-afterack"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "io.monix"      %% "monix"                    % "3.0.0-RC2",
  "io.monix"      %% "monix-kafka-1x"           % "1.0.0-RC2",
  "org.scalatest" %% "scalatest"                % "3.0.1" % Test,
  "net.manub"     %% "scalatest-embedded-kafka" % "1.0.0" % Test
)
