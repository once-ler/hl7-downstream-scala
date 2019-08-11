package com.eztier.datasource.exchange.runners

import java.net.URI

import com.eztier.common.Configuration.{conf, env}
import microsoft.exchange.webservices.data.core.ExchangeService
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName
import microsoft.exchange.webservices.data.core.enumeration.search.LogicalOperator
import microsoft.exchange.webservices.data.core.service.item.EmailMessage
import microsoft.exchange.webservices.data.core.service.schema.FolderSchema
import microsoft.exchange.webservices.data.credential.WebCredentials
import microsoft.exchange.webservices.data.property.complex.MessageBody
import microsoft.exchange.webservices.data.search.filter.SearchFilter
import microsoft.exchange.webservices.data.search.{FindFoldersResults, FolderView}

import scala.concurrent.{ExecutionContext}

object CommandRunner {

  val envConf = conf.getConfig(env)
  val url = envConf.getString("exchange.url")
  val email = envConf.getString("exchange.email")
  val pass = envConf.getString("exchange.password")

  val service = new ExchangeService(ExchangeVersion.Exchange2010_SP2)
  service.setUrl(new URI(url))

  val credentials = new WebCredentials(email, pass)
  service.setCredentials(credentials)

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

  def sendEmail(subject: String, body: String, recipients: String, folderName: String = "TestFolder")(implicit ec: ExecutionContext) = {
    val msg = new EmailMessage(service)
    msg.setSubject(subject)
    msg.setBody(MessageBody.getMessageBodyFromText(s"<pre>${body}</pre>"))

    for (n <- recipients.split(";"))
      msg.getToRecipients.add(n.trim)

    val findFoldersResults = getFindFoldersResults(service, folderName)

    try {
      findFoldersResults match {
        case Some(f) if f.getTotalCount > 0 =>
          msg.sendAndSaveCopy(f.getFolders.get(0).getId)
        case _ =>
          msg.sendAndSaveCopy
      }
    } catch {
      case e: Exception => e.printStackTrace
    }
  }

}
