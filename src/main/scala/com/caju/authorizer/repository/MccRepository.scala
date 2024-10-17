package com.caju.authorizer.repository

import zio.{Task, ZIO}

enum Balance:
	case Food, Meal, Cash

case class Mcc(code: String = "", balanceType: String = "Cash", merchant: String = "")

type MccCash = Mcc

case class MccCode(code: String, balanceType: Balance, merchant: String)

trait MccRepository {
    def lookup(code: String): Task[Option[Mcc]]

    def lookupByMerchantName(name: String): Task[Option[Mcc]]
}

object MccRepository {
	def lookup(code: String): ZIO[MccRepository, Throwable, Option[Mcc]] =
		ZIO.serviceWithZIO[MccRepository](_.lookup(code))

	def lookupByMerchantName(name: String): ZIO[MccRepository, Throwable, Option[Mcc]]  =
		ZIO.serviceWithZIO[MccRepository](_.lookupByMerchantName(name))
}
