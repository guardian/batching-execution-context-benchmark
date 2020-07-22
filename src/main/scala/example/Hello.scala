package example

import java.util.concurrent.Executors

import mouse.all._
import Impl._
import Model._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration.Inf

object Cli extends App {
    implicit val nonBatchingEc = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))
//  implicit val defaultEc = scala.concurrent.ExecutionContext.Implicits.global

  def parallelNesting(): Future[List[InvoiceOutput]] =
    Future("A00082327").flatMap { account => getInvoicesWithPaymentMethod(account) }

  (1 to 20) foreach { _ =>
    time {
      Await.result(parallelNesting(), Inf)
    }
  }
}
