package com.caju.authorizer.database

import com.caju.authorizer.repository.{Transaction, TransactionRepository}
import io.getquill.jdbczio.Quill
import io.getquill.*
import zio.*

import javax.sql.DataSource

case class TransactionTable(account: String, totalAmount: BigDecimal, mcc: String, merchant: String)

case class PersistentTransactionRepository(ds: DataSource) extends TransactionRepository:
	val ctx = new H2ZioJdbcContext(Escape)

	import ctx.*

	override def register(user: Transaction): Task[Unit] = {
		for
			_ <- ctx.run {
				quote {
					query[TransactionTable].insertValue {
						lift(TransactionTable(user.account, user.totalAmount, user.mcc, user.merchant))
					}
				}
			}
		yield ()
	}.provide(ZLayer.succeed(ds))

object PersistentTransactionRepository:
	def layer: ZLayer[Any, Throwable, PersistentTransactionRepository] =
		Quill.DataSource.fromPrefix("ConfigApp") >>>
			ZLayer.fromFunction(PersistentTransactionRepository(_))
