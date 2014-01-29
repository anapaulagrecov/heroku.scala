package com.heroku.platform.api

import com.heroku.platform.api.Request._

import Plan._

/** Plans represent different configurations of add-ons that may be added to apps. */
object Plan {
  import Plan.models._
  object models {
    case class PlanPrice(cents: Int, unit: String)
  }
  /** Info for existing plan. */
  case class Info(addon_service_id_or_name: String, plan_id_or_name: String) extends Request[Plan] {
    val expect: Set[Int] = expect200
    val endpoint: String = "/addon-services/%s/plans/%s".format(addon_service_id_or_name, plan_id_or_name)
    val method: String = GET
  }
  /** List existing plans. */
  case class List(addon_service_id_or_name: String, range: Option[String] = None) extends ListRequest[Plan] {
    val endpoint: String = "/addon-services/%s/plans".format(addon_service_id_or_name)
    val method: String = GET
    def nextRequest(nextRange: String): ListRequest[Plan] = this.copy(range = Some(nextRange))
  }
}

/** Plans represent different configurations of add-ons that may be added to apps. */
case class Plan(name: String, state: String, description: String, price: models.PlanPrice, id: String, default: Boolean, created_at: String, updated_at: String)

/** json serializers related to Plan */
trait PlanRequestJson {
  implicit def ToJsonPlanPrice: ToJson[models.PlanPrice]
}

/** json deserializers related to Plan */
trait PlanResponseJson {
  implicit def FromJsonPlanPrice: FromJson[models.PlanPrice]
  implicit def FromJsonPlan: FromJson[Plan]
  implicit def FromJsonListPlan: FromJson[collection.immutable.List[Plan]]
}