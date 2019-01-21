import monix.eval.Task
import monix.execution.Scheduler
import monix.execution.Scheduler.Implicits.global
import monix.kafka.config.{AutoOffsetReset, ObservableCommitOrder, ObservableCommitType}
import monix.kafka.{KafkaConsumerConfig, KafkaConsumerObservable, KafkaProducerConfig, KafkaProducerSink}
import monix.reactive.{Consumer, Observable}
import org.apache.kafka.clients.producer.ProducerRecord
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{MustMatchers, WordSpec}

class ConsumerTest extends WordSpec with MustMatchers with ScalaFutures with IntegrationPatience with KafkaTestKit {
  import ConsumerTest._

  "consumer2" should {
    "resume where consumer1 fails" in {
      // produce records into Kafka
      Observable
        .fromIterable(1 to 10)
        .map[ProducerRecord[String, Integer]](n => new ProducerRecord(topicName, n))
        .bufferIntrospective(1)
        .consumeWith(mkProducer)
        .runToFuture
        .futureValue

      // consumer1 consume the first 5 and fails at 5, commit at 4
      consumer1.completedL.runToFuture.failed.futureValue

      // consumer2 consume 5 and 6
      consumer2.take(2).consumeWith(Consumer.foldLeft(0)(_ + _)).runToFuture.futureValue mustBe (5 to 6).sum
    }
  }
}

object ConsumerTest {
  val topicName = "justnumber"

  val producerCfg = KafkaProducerConfig.default.copy(
    bootstrapServers = List("127.0.0.1:6001"),
    clientId = "producer-test"
  )

  val consumerCfg = KafkaConsumerConfig.default.copy(
    bootstrapServers = List("127.0.0.1:6001"),
    groupId = "consumer-test",
    enableAutoCommit = false,
    autoOffsetReset = AutoOffsetReset.Earliest,
    observableCommitOrder = ObservableCommitOrder.AfterAck,
    observableCommitType = ObservableCommitType.Sync,
    maxPollRecords = 1
  )

  val io = Scheduler.io("kafka-tests")

  val mkProducer = KafkaProducerSink[String, Integer](producerCfg, io)

  def mkConsumer = KafkaConsumerObservable[String, Integer](consumerCfg, List(topicName)).executeOn(io)

  def consumer1 = mkConsumer.mapEval { record =>
    val v = record.value()
    if (v == 5)
      Task.raiseError(new RuntimeException("Failed"))
    else
      Task.now(v)
  }

  def consumer2 = mkConsumer.mapEval(v => Task.now(v.value()))
}
