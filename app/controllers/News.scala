package controllers

import java.util.Date

import scala.Array.canBuildFrom

import org.squeryl.PrimitiveTypeMode.transaction

import play.api.data.Forms.date
import play.api.data.Forms.longNumber
import play.api.data.Forms.mapping
import play.api.data.Forms.nonEmptyText
import play.api.data.Forms.optional
import play.api.data.Form
import play.api.mvc.Controller

object News extends Controller with securesocial.core.SecureSocial {

  val newsForm = Form(
    mapping(
      "title" -> nonEmptyText,
      "text" -> nonEmptyText,
      "labelsAsString" -> nonEmptyText,
      "authorId" -> longNumber,
      "publishedDate" -> date("dd/MM/yyyy"),
      "hackathonId" -> optional(longNumber))(model.News.apply)(model.News.unapply))

  def index = UserAwareAction { implicit request =>
    transaction {
      Ok(views.html.news.index(model.News.all, request.user))
    }
  }

  def indexH(hid: Long) = UserAwareAction { implicit request =>
    transaction {
      Ok(views.html.news.indexH(model.Hackathon.lookup(hid), request.user))
    }
  }

  def view(id: Long) = UserAwareAction { implicit request =>
    transaction {
      Ok(views.html.news.view(model.News.lookup(id), request.user))
    }
  }

  def viewH(hid: Long, id: Long) = UserAwareAction { implicit request =>
    transaction {
      Ok(views.html.news.viewH(model.Hackathon.lookup(hid), model.News.lookup(id), request.user))
    }
  }

  def create = SecuredAction() { implicit request =>
    transaction {
      val news = new model.News(request.user.hackathonUserId)
      Ok(views.html.news.create(newsForm.fill(news), request.user))
    }
  }

  def createH(hid: Long) = SecuredAction() { implicit request =>
    transaction {
      val hackathon = model.Hackathon.lookup(hid)
      val news = new model.News(request.user.hackathonUserId, Some(hid))
      Ok(views.html.news.createH(hackathon, newsForm.fill(news), request.user))
    }
  }

  def save = SecuredAction() { implicit request =>
    newsForm.bindFromRequest.fold(
      errors => transaction {
        BadRequest(views.html.news.create(errors, request.user))
      },
      news => transaction {
        model.News.insert(news)
        Redirect(routes.News.index).flashing("status" -> "added", "title" -> news.title)
      })
  }

  def saveH(hid: Long) = SecuredAction() { implicit request =>
    newsForm.bindFromRequest.fold(
      errors => transaction {
        val hackathon = model.Hackathon.lookup(hid)
        BadRequest(views.html.news.createH(hackathon, errors, request.user))
      },
      news => transaction {
        model.News.insert(news)
        Redirect(routes.News.indexH(hid)).flashing("status" -> "added", "title" -> news.title)
      })
  }

  def edit(id: Long) = SecuredAction() { implicit request =>
    transaction {
      model.News.lookup(id).map { news =>
        Ok(views.html.news.edit(id, newsForm.fill(news.copy(labelsAsString = news.labels.map(_.value).mkString(","))), request.user))
      }.getOrElse {
        // no news found
        Redirect(routes.News.view(id)).flashing()
      }
    }
  }

  def editH(hid: Long, id: Long) = SecuredAction() { implicit request =>
    transaction {
      model.News.lookup(id).map { news =>
        Ok(views.html.news.editH(news.hackathon, id, newsForm.fill(news.copy(labelsAsString = news.labels.map(_.value).mkString(","))), request.user))
      }.getOrElse {
        // no news found
        Redirect(routes.News.viewH(hid, id)).flashing()
      }
    }
  }

  def update(id: Long) = SecuredAction() { implicit request =>
    newsForm.bindFromRequest.fold(
      errors => transaction {
        BadRequest(views.html.news.edit(id, errors, request.user))
      },
      news => transaction {
        model.News.update(id, news)
        Redirect(routes.News.index).flashing("status" -> "updated", "title" -> news.title)
      })
  }

  def updateH(hid: Long, id: Long) = SecuredAction() { implicit request =>
    newsForm.bindFromRequest.fold(
      errors => transaction {
        val hackathon = model.Hackathon.lookup(hid)
        BadRequest(views.html.news.editH(hackathon, id, errors, request.user))
      },
      news => transaction {
        model.News.update(id, news)
        Redirect(routes.News.indexH(hid)).flashing("status" -> "updated", "title" -> news.title)
      })
  }

  def delete(id: Long) = SecuredAction() { implicit request =>
    transaction {
      model.News.delete(id)
    }
    Redirect(routes.News.index).flashing("status" -> "deleted")
  }

  def deleteH(hid: Long, id: Long) = SecuredAction() { implicit request =>
    transaction {
      model.News.delete(id)
    }
    Redirect(routes.News.indexH(hid)).flashing("status" -> "deleted")
  }

}