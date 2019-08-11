package com.eztier.datasource.test

import java.net.URI

import com.eztier.common.Configuration.{conf, env}
// import microsoft.exchange.webservices.data.core.ExchangeService
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion
import microsoft.exchange.webservices.data.core.service.folder.Folder
// import microsoft.exchange.webservices.data.credential.ExchangeCredentials
import microsoft.exchange.webservices.data.credential.WebCredentials

// Folder
import microsoft.exchange.webservices.data.core.ExchangeService
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName
import microsoft.exchange.webservices.data.core.enumeration.search.LogicalOperator
import microsoft.exchange.webservices.data.core.service.schema.FolderSchema
import microsoft.exchange.webservices.data.search.FindFoldersResults
import microsoft.exchange.webservices.data.search.FolderView
import microsoft.exchange.webservices.data.search.filter.SearchFilter

// Email
import microsoft.exchange.webservices.data.core.service.item.EmailMessage
import microsoft.exchange.webservices.data.property.complex.MessageBody

import org.scalatest.{FunSpec, Matchers}

class TestExchangeSpec extends FunSpec with Matchers {

  val folderName ="TestFolder"

  def getFindFoldersResults(service: ExchangeService, folderName: String): Option[FindFoldersResults] = {
    val fv = new FolderView(10)
    try
      Some(service.findFolders(
        WellKnownFolderName.Inbox,
        new SearchFilter.SearchFilterCollection(LogicalOperator.Or,
          new SearchFilter.IsEqualTo(FolderSchema.DisplayName, folderName)),
        fv)
      )
    catch {
      case e: Exception =>
        e.printStackTrace
        None
    }
  }

  describe ("MS Exchange Suite") {
    val envConf = conf.getConfig(env)
    val url = envConf.getString("exchange.url")

    val email = envConf.getString("exchange.email")
    val pass = envConf.getString("exchange.password")
    val recipients = envConf.getString("exchange.recipients")

    val service = new ExchangeService(ExchangeVersion.Exchange2010_SP2)
    service.setUrl(new URI(url))
    // or service.autodiscoverUrl("<your_email_address>")

    val credentials = new WebCredentials(email, pass)
    service.setCredentials(credentials)

    it ("Should create a folder") {
      val findFoldersResults = getFindFoldersResults(service, folderName)

      findFoldersResults match {
        case Some(f) if f.getTotalCount == 0 =>
          import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName
          val folder = new Folder(service)
          folder.setDisplayName(folderName)
          folder.save(WellKnownFolderName.Inbox)
          Some()
        case _ => None
      }
    }

    it ("Should send a simple message") {
      val msg = new EmailMessage(service)
      msg.setSubject("Some Alert")
      msg.setBody(MessageBody.getMessageBodyFromText("Foo bar"))
      msg.getToRecipients.add(recipients)

      val findFoldersResults = getFindFoldersResults(service, folderName)

      findFoldersResults match {
        case Some(f) if f.getTotalCount > 0 =>
          msg.sendAndSaveCopy(f.getFolders.get(0).getId)
        case _ =>
          msg.sendAndSaveCopy
      }
    }

  }
}
