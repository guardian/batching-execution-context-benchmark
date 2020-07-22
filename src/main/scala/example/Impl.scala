package example

import scalaj.http.{BaseHttp, HttpOptions}
import mouse.all._
import Model._

import scala.concurrent.{ExecutionContext, Future}

object Impl {
  def time[R](block: => R): R = {
    val t0 = System.nanoTime()
    val result = block
    val t1 = System.nanoTime()
    println((t1 - t0) / 1000000000d) // seconds
    result
  }

  val zuoraApiHost: String = "https://rest.apisandbox.zuora.com"

  object HttpWithLongTimeout extends BaseHttp(
    options = Seq(
      HttpOptions.connTimeout(5000),
      HttpOptions.readTimeout(5 * 60 * 1000),
      HttpOptions.followRedirects(false)
    )
  )

  private lazy val token = sys.env("TOKEN")
  def accessToken(): String = {
    // FIXME: Implement auth
    token
  }

  def getInvoices(account: String): List[Invoice] = {
    HttpWithLongTimeout(s"$zuoraApiHost/v1/transactions/invoices/accounts/$account")
      .header("Authorization", s"Bearer ${accessToken()}")
      .asString
      .body
      .|>(read[Invoices](_))
      .invoices
  }

  def getPayments(account: String): List[Payment] = {
    HttpWithLongTimeout(s"$zuoraApiHost/v1/transactions/payments/accounts/$account")
      .header("Authorization", s"Bearer ${accessToken()}")
      .asString
      .body
      .|>(read[Payments](_))
      .payments
  }

  def getPaymentMethods(accountId: String): List[PaymentMethod] = {
    HttpWithLongTimeout(s"$zuoraApiHost/v1/action/query")
      .header("Authorization", s"Bearer ${accessToken()}")
      .header("Content-Type", "application/json")
      .postData(s"""{"queryString": "select BankTransferType, CreditCardExpirationMonth, CreditCardExpirationYear, BankTransferAccountNumberMask, LastFailedSaleTransactionDate, LastTransactionDateTime, LastTransactionStatus, Name, NumConsecutiveFailures, PaymentMethodStatus, Type, ID, MandateID, PaypalBAID, SecondTokenID, TokenID, AccountID, Active, Country, CreatedById, CreatedDate, CreditCardType, DeviceSessionId, IdentityNumber, MandateCreationDate, MandateReceived, MandateUpdateDate, MaxConsecutivePaymentFailures, PaymentRetryWindow, TotalNumberOfErrorPayments, TotalNumberOfProcessedPayments, UpdatedById, UpdatedDate, UseDefaultRetryRule, CreditCardMaskNumber from PaymentMethod where AccountId = '$accountId'"}""")
      .method("POST")
      .asString
      .body
      .|>(read[PaymentMethods](_))
      .records
  }

  def getInvoicesWithPaymentMethod(account: String = "A00082327")(implicit ec: ExecutionContext): Future[List[InvoiceOutput]] = {
    val invoicesF = Future(getInvoices(account))
    val paymentsF = Future(getPayments(account))
    val paymentMethodsF = Future(getPaymentMethods("2c92c0f87250379b017255aa8d6f63f7"))

    for {
      invoices       <- invoicesF
      payments       <- paymentsF
      paymentMethods <- paymentMethodsF
    } yield {
      val paymentMethodIdByInvoiceId: Map[String, String] =
        payments
          .flatMap { payment =>
            payment
              .paidInvoices
              .headOption
              .map { invoice => invoice.invoiceId -> payment.paymentMethodId }
          }.toMap

      val paymentMethodById: Map[String, PaymentMethod] =
        paymentMethods.map(paymentMethod => paymentMethod.Id -> paymentMethod).toMap

      val paymentMethodByInvoiceId: Map[String, PaymentMethod] =
        invoices.map { invoice =>
          invoice.id -> paymentMethodById(paymentMethodIdByInvoiceId(invoice.id))
        }.toMap

      invoices.map { invoice =>
        InvoiceOutput(
          product = invoice.invoiceItems.head.productName, // FIXME
          date = invoice.invoiceDate,
          paymentMethod = paymentMethodByInvoiceId(invoice.id),
          price = invoice.amount.toDouble,
          downloadUrl = invoice.invoiceFiles.head.pdfFileUrl // FIXME
        )
      }

    }
  }
}
