package com.caju.authorizer.database

import com.caju.authorizer.repository.{Mcc, MccRepository}
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

import javax.sql.DataSource

case class MccTable(code: String, balanceType: String, merchant: String)

case class PersistentMccRepository(ds: DataSource) extends MccRepository:
	val ctx = new H2ZioJdbcContext(Escape)

	import ctx.*

	override def lookup(code: String): Task[Option[Mcc]] =
		ctx.run {
				quote {
					query[MccTable]
						.filter(p => p.code == lift(code))
						.map(u => Mcc(u.code, u.balanceType, u.merchant))
				}
			}
			.provide(ZLayer.succeed(ds))
			.map(_.headOption)

	override def lookupByMerchantName(name: String): Task[Option[Mcc]] =
		ctx.run {
				quote {
					query[MccTable]
						.filter(p => p.merchant == lift(name))
						.map(u => Mcc(u.code, u.balanceType, u.merchant))
				}
			}
			.provide(ZLayer.succeed(ds))
			.map(_.headOption)

object PersistentMccRepository:
	def layer: ZLayer[Any, Throwable, PersistentMccRepository] =
		Quill.DataSource.fromPrefix("ConfigApp") >>>
			ZLayer.fromFunction(PersistentMccRepository(_))
