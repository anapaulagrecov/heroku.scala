package com.heroku.platform.api

import com.heroku.platform.api.Request._

import AppTransfer._

/** An app transfer represents a two party interaction for transferring ownership of an app. */
object AppTransfer {
  import AppTransfer.models._
  object models {
    case class CreateAppTransferBody(app: String, recipient: String)
    case class UpdateAppTransferBody(state: String)
    case class AppTransferRecipient(email: String, id: String)
    case class AppTransferApp(name: String, id: String)
    case class AppTransferOwner(email: String, id: String)
  }
  /** Create a new app transfer. */
  case class Create(app_id_or_name: String, recipient_email_or_id: String) extends RequestWithBody[models.CreateAppTransferBody, AppTransfer] {
    val expect: Set[Int] = expect201
    val endpoint: String = "/account/app-transfers"
    val method: String = POST
    val body: models.CreateAppTransferBody = models.CreateAppTransferBody(app_id_or_name, recipient_email_or_id)
  }
  /** Delete an existing app transfer */
  case class Delete(app_transfer_id_or_name: String) extends Request[AppTransfer] {
    val expect: Set[Int] = expect200
    val endpoint: String = "/account/app-transfers/%s".format(app_transfer_id_or_name)
    val method: String = DELETE
  }
  /** Info for existing app transfer. */
  case class Info(app_transfer_id_or_name: String) extends Request[AppTransfer] {
    val expect: Set[Int] = expect200
    val endpoint: String = "/account/app-transfers/%s".format(app_transfer_id_or_name)
    val method: String = GET
  }
  /** List existing apps transfers. */
  case class List(range: Option[String] = None) extends ListRequest[AppTransfer] {
    val endpoint: String = "/account/app-transfers"
    val method: String = GET
    def nextRequest(nextRange: String): ListRequest[AppTransfer] = this.copy(range = Some(nextRange))
  }
  /** Update an existing app transfer. */
  case class Update(app_transfer_id_or_name: String, state: String) extends RequestWithBody[models.UpdateAppTransferBody, AppTransfer] {
    val expect: Set[Int] = expect200
    val endpoint: String = "/account/app-transfers/%s".format(app_transfer_id_or_name)
    val method: String = PATCH
    val body: models.UpdateAppTransferBody = models.UpdateAppTransferBody(state)
  }
}

/** An app transfer represents a two party interaction for transferring ownership of an app. */
case class AppTransfer(recipient: models.AppTransferRecipient, state: String, id: String, app: models.AppTransferApp, created_at: String, owner: models.AppTransferOwner, updated_at: String)

/** json serializers related to AppTransfer */
trait AppTransferRequestJson {
  implicit def ToJsonCreateAppTransferBody: ToJson[models.CreateAppTransferBody]
  implicit def ToJsonUpdateAppTransferBody: ToJson[models.UpdateAppTransferBody]
  implicit def ToJsonAppTransferRecipient: ToJson[models.AppTransferRecipient]
  implicit def ToJsonAppTransferApp: ToJson[models.AppTransferApp]
  implicit def ToJsonAppTransferOwner: ToJson[models.AppTransferOwner]
}

/** json deserializers related to AppTransfer */
trait AppTransferResponseJson {
  implicit def FromJsonAppTransferRecipient: FromJson[models.AppTransferRecipient]
  implicit def FromJsonAppTransferApp: FromJson[models.AppTransferApp]
  implicit def FromJsonAppTransferOwner: FromJson[models.AppTransferOwner]
  implicit def FromJsonAppTransfer: FromJson[AppTransfer]
  implicit def FromJsonListAppTransfer: FromJson[collection.immutable.List[AppTransfer]]
}