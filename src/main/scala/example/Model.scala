package example

import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ofPattern
import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

/**
 * Needed for upickle to handle optional fields
 * http://www.lihaoyi.com/upickle/#CustomConfiguration
 */
class OptionPickler extends upickle.AttributeTagged {
  implicit def optionWriter[T: Writer]: Writer[Option[T]] =
    implicitly[Writer[T]].comap[Option[T]] {
      case None => null.asInstanceOf[T]
      case Some(x) => x
    }

  implicit def optionReader[T: Reader]: Reader[Option[T]] = {
    new Reader.Delegate[Any, Option[T]](implicitly[Reader[T]].map(Some(_))){
      override def visitNull(index: Int) = None
    }
  }
}

/**
 * Data models and JSON codecs
 */
object Model extends OptionPickler {
  case class Config(clientId: String, clientSecret: String)
  case class AccessToken(access_token: String)

  case class Invoices(
    invoices: List[Invoice],
    success: Boolean
  )
  case class Invoice(
    id: String,
    accountId: String,
    accountNumber: String,
    accountName: String,
    invoiceDate: LocalDate,
    invoiceNumber: String,
    dueDate: LocalDate,
    invoiceTargetDate: LocalDate,
    amount: BigDecimal,
    balance: BigDecimal,
    creditBalanceAdjustmentAmount: BigDecimal,
    createdBy: String,
    status: String,
    body: String,
    invoiceItems: List[InvoiceItem],
    invoiceFiles: List[InvoiceFile],
  )
  case class InvoiceItem(
    id: String,
    subscriptionName: String,
    subscriptionId: String,
    serviceStartDate: LocalDate,
    serviceEndDate: LocalDate,
    chargeAmount: BigDecimal,
    chargeDescription: String,
    chargeName: String,
    chargeId: String,
    productName: String,
    quantity: BigDecimal,
    taxAmount: BigDecimal,
    unitOfMeasure: String,
    chargeDate: String, // FIXME this has different format from other kinds of localdatetimes
    chargeType: String,
    processingType: String,
    appliedToItemId: Option[String]
  )
  case class InvoiceFile(
    id: String,
    versionNumber: Int,
    pdfFileUrl: String
  )

  case class InvoiceOutput(
    product: String,
    date: LocalDate,
    paymentMethod: PaymentMethod,
    price: Double,
    downloadUrl: String
  )
  case class InvoicesOutput(
    invoices: List[InvoiceOutput]
  )

  case class Payment(
    id: String,
    accountId: String,
    accountNumber: String,
    accountName: String,
    `type`: String,
    effectiveDate: LocalDate,
    paymentNumber: String,
    paymentMethodId: String,
    amount: BigDecimal,
    paidInvoices: List[PaidInvoice],
    gatewayTransactionNumber: String,
    status: String,
  )

  case class PaidInvoice(
    invoiceId: String,
    invoiceNumber: String,
    appliedPaymentAmount: BigDecimal
  )

  case class Payments(
    payments: List[Payment],
    success: Boolean
  )

  case class PaymentMethod(
    Id: String,
    AccountId: String,
    LastTransactionDateTime: LocalDateTime,
    NumConsecutiveFailures: Int,
    TotalNumberOfProcessedPayments: Int,
    UpdatedById: String,
    CreatedDate: LocalDateTime,
    UseDefaultRetryRule: Boolean,
    LastTransactionStatus: String,
    PaymentMethodStatus: String,
    UpdatedDate: LocalDateTime,
    CreatedById: String,
    Type: String,
    TotalNumberOfErrorPayments: Int,
    MandateID: Option[String] = None,
    BankTransferAccountNumberMask: Option[String] = None,
    BankTransferType: Option[String] = None,
    Active: Boolean
  )

  case class PaymentMethods(
    records: List[PaymentMethod],
    done: Boolean,
    size: Int
  )

  implicit val bigDecimalRW: ReadWriter[BigDecimal] = readwriter[Double].bimap[BigDecimal](_.toDouble, double => BigDecimal(double.toString))
  implicit val localDateRW: ReadWriter[LocalDate] = readwriter[String].bimap[LocalDate](_.toString, LocalDate.parse(_, ofPattern("yyyy-MM-dd")))
//  implicit val localDateTimeRW: ReadWriter[LocalDateTime] = readwriter[String].bimap[LocalDateTime](_.toString, LocalDateTime.parse(_, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
  implicit val localDateTimeRW: ReadWriter[LocalDateTime] = readwriter[String].bimap[LocalDateTime](_.toString, LocalDateTime.parse(_, DateTimeFormatter.ISO_OFFSET_DATE_TIME)) // ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")


  implicit val configRW: ReadWriter[Config] = macroRW
  implicit val accessTokenRW: ReadWriter[AccessToken] = macroRW

  implicit val invoicesRW: ReadWriter[Invoices] = macroRW
  implicit val invoiceRW: ReadWriter[Invoice] = macroRW
  implicit val invoiceItemRW: ReadWriter[InvoiceItem] = macroRW
  implicit val invoiceFileRW: ReadWriter[InvoiceFile] = macroRW

  implicit val paymentRW: ReadWriter[Payment] = macroRW
  implicit val paidInvoiceRW: ReadWriter[PaidInvoice] = macroRW
  implicit val paymentsRW: ReadWriter[Payments] = macroRW

  implicit val paymentMethodRW: ReadWriter[PaymentMethod] = macroRW
  implicit val paymentMethodsRW: ReadWriter[PaymentMethods] = macroRW

  implicit val invoiceOutputRW: ReadWriter[InvoiceOutput] = macroRW

  // https://docs.aws.amazon.com/apigateway/latest/developerguide/set-up-lambda-proxy-integrations.html#api-gateway-simple-proxy-for-lambda-input-format
  case class ApiGatewayInput(body: String)

  // https://aws.amazon.com/premiumsupport/knowledge-center/malformed-502-api-gateway/
  case class ApiGatewayOutput(statusCode: Int, body: String)

  implicit val awsBodyRW: ReadWriter[ApiGatewayInput] = macroRW
  implicit val apiGatewayOutputRW: ReadWriter[ApiGatewayOutput] = macroRW
}
