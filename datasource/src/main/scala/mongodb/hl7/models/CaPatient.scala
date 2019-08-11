package com.eztier.datasource.mongodb.hl7.models

import java.text.SimpleDateFormat
import java.util.Date

import com.eztier.hl7mock.types._
import org.bson.{BsonReader, BsonWriter}
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.joda.time.DateTime
import org.mongodb.scala.bson.annotations.BsonProperty
import org.mongodb.scala.bson.codecs.Macros

import scala.util.{Failure, Success, Try}

case class CaPatientPhoneInfoMongo
(
  @BsonProperty("number") Number: String = "",
  @BsonProperty("type") Type: String = ""
)

case class CaPatientEmailInfoMongo
(
  @BsonProperty("email") Email: String = "",
  @BsonProperty("type") Type: String = ""
)

case class CaPatientIdTypeMongo
(
  @BsonProperty("id") Id: String = "",
  @BsonProperty("type") Type: String = ""
)

case class CaPatientNameComponentsMongo
(
  @BsonProperty("academic") Academic: String = "",
  @BsonProperty("firstName") FirstName: String = "",
  @BsonProperty("givenName") GivenName: String = "",
  @BsonProperty("initials") Initials: String = "",
  @BsonProperty("lastName") LastName: String = "",
  @BsonProperty("lastNameFromSpouse") LastNameFromSpouse: String = "",
  @BsonProperty("lastNamePrefix") LastNamePrefix: String = "",
  @BsonProperty("middleName") MiddleName: String = "",
  @BsonProperty("preferredName") PreferredName: String = "",
  @BsonProperty("preferredNameType") PreferredNameType: String = "",
  @BsonProperty("spouseLastNameFirst") SpouseLastNameFirst: String = "",
  @BsonProperty("spouseLastNamePrefix") SpouseLastNamePrefix: String = "",
  @BsonProperty("suffix") Suffix: String = "",
  @BsonProperty("title") Title: String = ""
)

case class CaPatientAddressMongo
(
  @BsonProperty("city") City: String = "",
  @BsonProperty("country") Country: String = "",
  @BsonProperty("county") County: String = "",
  @BsonProperty("district") District: String = "",
  @BsonProperty("email") Email: Seq[CaPatientEmailInfoMongo] = Seq(),
  @BsonProperty("houseNumber") HouseNumber: String = "",
  @BsonProperty("phoneNumbers") PhoneNumbers: Seq[CaPatientPhoneInfoMongo] = Seq(),
  @BsonProperty("postalCode") PostalCode: String = "",
  @BsonProperty("state") State: String = "",
  @BsonProperty("street") Street: Seq[String] = Seq(),
  @BsonProperty("type") Type: String = ""
)

case class CaPatientCareTeamMemberMongo
(
  @BsonProperty("ids") Ids: Seq[CaPatientIdTypeMongo] = Seq(),
  @BsonProperty("name") Name: String = "",
  @BsonProperty("type") Type: String = ""
)

case class CaPatientEmergencyContactMongo
(
  @BsonProperty("legalGuardian") LegalGuardian: String = "",
  @BsonProperty("name") Name: String = "",
  @BsonProperty("phoneNumbers") PhoneNumbers: Seq[CaPatientPhoneInfoMongo] = Seq(),
  @BsonProperty("relation") Relation: String = ""
)

case class CaPatientEmploymentInformationMongo
(
  @BsonProperty("employerName") EmployerName: String = "",
  @BsonProperty("occupation") Occupation: String = "",
  @BsonProperty("phoneNumbers") PhoneNumbers: Seq[CaPatientPhoneInfoMongo] = Seq()
)

case class CaPatientRaceMongo
(
  @BsonProperty("code") Code: String = "",
  @BsonProperty("display") Display: String = ""
)

case class CaPatientEthnicityMongo
(
  @BsonProperty("code") Code: String = "",
  @BsonProperty("display") Display: String = ""
)

case class CaPatientGenderMongo
(
  @BsonProperty("code") Code: String = "",
  @BsonProperty("display") Display: String = ""
)

case class CaPatientMongo
(
  @BsonProperty("addresses") Addresses: Seq[CaPatientAddressMongo] = Seq(),
  @BsonProperty("aliases") Aliases: Seq[String] = Seq(),
  @BsonProperty("careTeam") CareTeam: Seq[CaPatientCareTeamMemberMongo] = Seq(),
  @BsonProperty("confidentialName") ConfidentialName: String = "",
  // @BsonProperty("createDate") CreateDate: Date = new Date(),
  // @BsonProperty("dateOfBirth") DateOfBirth: Date = new DateTime(1900, 1, 1, 0, 0, 0).toDate,
  @BsonProperty("createDate") CreateDate: String = "",
  @BsonProperty("dateOfBirth") DateOfBirth: String = "",
  @BsonProperty("emergencyContacts") EmergencyContacts: Seq[CaPatientEmergencyContactMongo] = Seq(),
  @BsonProperty("employmentInformation") EmploymentInformation: CaPatientEmploymentInformationMongo = CaPatientEmploymentInformationMongo(),
  @BsonProperty("ethnicity") Ethnicity: Seq[CaPatientEthnicityMongo] = Seq(),
  @BsonProperty("gender") Gender: CaPatientGenderMongo = CaPatientGenderMongo(),
  @BsonProperty("historicalIds") HistoricalIds: Seq[CaPatientIdTypeMongo] = Seq(),
  @BsonProperty("homeDeployment") HomeDeployment: String = "",
  @BsonProperty("id") Id: String = "",
  @BsonProperty("ids") Ids: Seq[CaPatientIdTypeMongo] = Seq(),
  @BsonProperty("maritalStatus") MaritalStatus: String = "",
  @BsonProperty("mrn") Mrn: String = "",
  @BsonProperty("name") Name: String = "",
  @BsonProperty("nameComponents") NameComponents: Seq[CaPatientNameComponentsMongo] = Seq(),
  @BsonProperty("nationalIdentifier") NationalIdentifier: String = "",
  @BsonProperty("race") Race: Seq[CaPatientRaceMongo] = Seq(),
  @BsonProperty("rank") Rank: String = "",
  @BsonProperty("status") Status: String = ""
)

object CaPatientMongo {
  implicit def caPatientPhoneInfoMongoToCaPatientPhoneInfo(in: CaPatientPhoneInfoMongo): CaPatientPhoneInfo = {
    CaPatientPhoneInfo(Number = in.Number, Type = in.Type)
  }

  implicit def caPatientEmailInfoMongoToCaPatientEmailInfo(in: CaPatientEmailInfoMongo): CaPatientEmailInfo = {
    CaPatientEmailInfo(Email = in.Email, Type = in.Type)
  }

  implicit def caPatientIdTypeMongoToCaPatientIdType(in: CaPatientIdTypeMongo): CaPatientIdType = {
    CaPatientIdType(Id = in.Id, Type = in.Type)
  }

  implicit def caPatientNameComponentsMongoToCaPatientNameComponents(in: CaPatientNameComponentsMongo): CaPatientNameComponents = {
    CaPatientNameComponents(
      Academic = in.Academic,
      FirstName = in.FirstName,
      GivenName = in.GivenName,
      Initials = in.Initials,
      LastName = in.LastName,
      LastNameFromSpouse = in.LastNameFromSpouse,
      LastNamePrefix = in.LastNamePrefix,
      MiddleName = in.MiddleName,
      PreferredName = in.PreferredName,
      PreferredNameType = in.PreferredNameType,
      SpouseLastNameFirst = in.SpouseLastNameFirst,
      SpouseLastNamePrefix = in.SpouseLastNamePrefix,
      Suffix = in.Suffix,
      Title = in.Title
    )
  }

  implicit def caPatientAddressMongoToCaPatientAddressMongo(in: CaPatientAddressMongo): CaPatientAddress = {
    CaPatientAddress(
      City = in.City,
      Country = in.Country,
      County = in.County,
      District = in.District,
      Email = in.Email.map{
        a =>
          val b: CaPatientEmailInfo = a
          b
      },
      HouseNumber = in.HouseNumber,
      PhoneNumbers = in.PhoneNumbers.map{
        a =>
          val b: CaPatientPhoneInfo = a
          b
      },
      PostalCode = in.PostalCode,
      State = in.State,
      Street = in.Street,
      Type = in.Type
    )
  }

  implicit def caPatientCareTeamMemberMongoToCaPatientCareTeamMember(in: CaPatientCareTeamMemberMongo): CaPatientCareTeamMember = {
    CaPatientCareTeamMember(
      Ids = in.Ids.map{
        a =>
          val b: CaPatientIdType = a
          b
      },
      Name = in.Name,
      Type = in.Type
    )
  }

  implicit def caPatientEmergencyContactMongoToCaPatientEmergencyContactMongo(in: CaPatientEmergencyContactMongo): CaPatientEmergencyContact = {
    CaPatientEmergencyContact(
      LegalGuardian = in.LegalGuardian,
      Name = in.Name,
      PhoneNumbers = in.PhoneNumbers.map{
        a =>
          val b: CaPatientPhoneInfo = a
          b
      },
      Relation = in.Relation
    )
  }

  implicit def CaPatientEmploymentInformationMongoToCaPatientEmploymentInformation(in: CaPatientEmploymentInformationMongo): CaPatientEmploymentInformation = {
    CaPatientEmploymentInformation(
      EmployerName = in.EmployerName,
      Occupation =  in.Occupation,
      PhoneNumbers = in.PhoneNumbers.map {
        a =>
          val b: CaPatientPhoneInfo = a
          b
      }
    )
  }

  implicit def caPatientRaceMongoToCaPatientRaceMongo(in: CaPatientRaceMongo): CaPatientRace = {
    CaPatientRace(Code = in.Code, Display = in.Display)
  }

  implicit def caPatientEthnicityMongoToCaPatientEthnicity(in: CaPatientEthnicityMongo): CaPatientEthnicity = {
    CaPatientEthnicity(Code = in.Code, Display = in.Display)
  }

  implicit def CaPatientGenderMongoToCaPatientGender(in: CaPatientGenderMongo): CaPatientGender = {
    CaPatientGender(Code = in.Code, Display = in.Display)
  }

  implicit def caPatientMongoToCaPatient(in: CaPatientMongo): CaPatient = {
    val createDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    val dateOfBirthFormat = new SimpleDateFormat("yyyy-MM-dd")

    CaPatient(
      Addresses =  in.Addresses.map {
        a =>
          val b: CaPatientAddress = a
          b
      },
      Aliases = in.Aliases,
      CareTeam = in.CareTeam.map {
        a =>
          val b: CaPatientCareTeamMember = a
          b
      },
      ConfidentialName = in.ConfidentialName,
      CreateDate = Try(createDateFormat.parse(in.CreateDate)) match {
        case Success(a) => a
        case Failure(e) => new Date()
      },
      DateOfBirth = Try(dateOfBirthFormat.parse(in.DateOfBirth)) match {
        case Success(a) => a
        case Failure(e) => new Date()
      },
      EmergencyContacts = in.EmergencyContacts.map {
        a =>
          val b: CaPatientEmergencyContact = a
          b
      },
      EmploymentInformation = in.EmploymentInformation,
      Ethnicity = in.Ethnicity.map {
        a =>
          val b: CaPatientEthnicity = a
          b
      },
      Gender = in.Gender,
      HistoricalIds = in.HistoricalIds.map {
        a =>
          val b: CaPatientIdType = a
          b
      },
      HomeDeployment = in.HomeDeployment,
      Id = in.Id,
      Ids = in.Ids.map {
        a =>
          val b: CaPatientIdType = a
          b
      },
      MaritalStatus = in.MaritalStatus,
      Mrn = in.Mrn,
      Name = in.Name,
      NameComponents = in.NameComponents.map {
        a =>
          val b: CaPatientNameComponents = a
          b
      },
      NationalIdentifier = in.NationalIdentifier,
      Race = in.Race.map {
        a =>
          val b: CaPatientRace = a
          b
      },
      Rank = in.Rank,
      Status = in.Status
    )
  }
}
