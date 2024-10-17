package com.caju.authorizer.database

import com.caju.authorizer.repository.{Transaction, TransactionRepository}
import io.getquill.{Escape, H2ZioJdbcContext}
import io.getquill.jdbczio.Quill
import io.getquill.*
import zio.*

import java.util.UUID
import javax.sql.DataSource

case class TransactionTable(uuid: UUID, account: String, totalAmount: Double, mcc: String, merchant: String)

case class PersistentTransactionRepository(ds: DataSource) extends TransactionRepository:
	val ctx = new H2ZioJdbcContext(Escape)

	import ctx._

	override def register(user: Transaction): Task[String] = {
		for
			id <- Random.nextUUID
			_ <- ctx.run {
				quote {
					query[TransactionTable].insertValue {
						lift(TransactionTable(id, user.account, user.totalAmount, user.mcc, user.merchant))
					}
				}
			}
		yield id.toString
	}.provide(ZLayer.succeed(ds))

object PersistentTransactionRepository:
	def layer: ZLayer[Any, Throwable, PersistentTransactionRepository] =
		Quill.DataSource.fromPrefix("ConfigApp") >>>
			ZLayer.fromFunction(PersistentTransactionRepository(_))
