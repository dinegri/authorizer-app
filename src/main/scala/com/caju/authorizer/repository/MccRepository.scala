package com.caju.authorizer.repository

import com.caju.authorizer.domain.Mcc
import zio.{Task, ZIO}

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
