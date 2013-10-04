package com.heroku.platform.api

import com.heroku.platform.api.Request._

import HerokuApp._

object HerokuApp {
  import HerokuApp.models._
  object models {
    case class CreateHerokuAppBody(name: Option[String] = None, region: Option[RegionIdentity] = None, stack: Option[String] = None)
    case class UpdateHerokuAppBody(maintenance: Option[Boolean] = None, name: Option[String] = None)
    case class HerokuAppStack(id: String, name: String)
    case class HerokuAppRegion(id: String, name: String)
    case class HerokuAppOwner(email: String, id: String)
  }
  case class Create(name: Option[String] = None, region: Option[RegionIdentity] = None, stack: Option[String] = None) extends RequestWithBody[models.CreateHerokuAppBody, HerokuApp] {
    val expect: Set[Int] = expect201
    val endpoint: String = "/apps"
    val method: String = POST
    val body: models.CreateHerokuAppBody = models.CreateHerokuAppBody(name, region, stack)
  }
  case class Delete(app_id_or_name: String) extends Request[HerokuApp] {
    val expect: Set[Int] = expect200
    val endpoint: String = "/apps/%s".format(app_id_or_name)
    val method: String = DELETE
  }
  case class Info(app_id_or_name: String) extends Request[HerokuApp] {
    val expect: Set[Int] = expect200
    val endpoint: String = "/apps/%s".format(app_id_or_name)
    val method: String = GET
  }
  case class List(range: Option[String] = None) extends ListRequest[HerokuApp] {
    val endpoint: String = "/apps"
    val method: String = GET
    def nextRequest(nextRange: String): ListRequest[HerokuApp] = this.copy(range = Some(nextRange))
  }
  case class Update(app_id_or_name: String, maintenance: Option[Boolean] = None, name: Option[String] = None) extends RequestWithBody[models.UpdateHerokuAppBody, HerokuApp] {
    val expect: Set[Int] = expect200
    val endpoint: String = "/apps/%s".format(app_id_or_name)
    val method: String = PATCH
    val body: models.UpdateHerokuAppBody = models.UpdateHerokuAppBody(maintenance, name)
  }
}

case class HerokuApp(name: String, repo_size: Option[Int], git_url: String, slug_size: Option[Int], maintenance: Boolean, id: String, released_at: Option[String], web_url: String, stack: Option[String], region: models.HerokuAppRegion, created_at: String, owner: models.HerokuAppOwner, updated_at: String, archived_at: Option[String], buildpack_provided_description: Option[String])

case class HerokuAppIdentity(id: Option[String], name: Option[String])

case object HerokuAppIdentity {
  def byId(id: String) = HerokuAppIdentity(Some(id), None)
  def byName(name: String) = HerokuAppIdentity(None, Some(name))
}

trait HerokuAppRequestJson {
  implicit def ToJsonCreateHerokuAppBody: ToJson[models.CreateHerokuAppBody]
  implicit def ToJsonUpdateHerokuAppBody: ToJson[models.UpdateHerokuAppBody]
  implicit def ToJsonHerokuAppStack: ToJson[models.HerokuAppStack]
  implicit def ToJsonHerokuAppRegion: ToJson[models.HerokuAppRegion]
  implicit def ToJsonHerokuAppOwner: ToJson[models.HerokuAppOwner]
  implicit def ToJsonHerokuAppIdentity: ToJson[HerokuAppIdentity]
}

trait HerokuAppResponseJson {
  implicit def FromJsonHerokuAppStack: FromJson[models.HerokuAppStack]
  implicit def FromJsonHerokuAppRegion: FromJson[models.HerokuAppRegion]
  implicit def FromJsonHerokuAppOwner: FromJson[models.HerokuAppOwner]
  implicit def FromJsonHerokuApp: FromJson[HerokuApp]
  implicit def FromJsonListHerokuApp: FromJson[collection.immutable.List[HerokuApp]]
}