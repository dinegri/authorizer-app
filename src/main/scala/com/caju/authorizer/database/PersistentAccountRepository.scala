package com.caju.authorizer.database

import com.caju.authorizer.repository.*
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

import javax.sql.DataSource

case class AccountTable(id: String, balanceFood: BigDecimal, balanceMeal: BigDecimal, balanceCash: BigDecimal)

case class PersistentAccountRepository(ds: DataSource) extends AccountRepository:
	val ctx = new H2ZioJdbcContext(Escape)

	import ctx.*

	override def register(user: Account): Task[String] = {
		for
			_ <- ctx.run {
				quote {
					query[AccountTable].insertValue {
						lift(AccountTable(user.id, user.balanceFood, user.balanceMeal, user.balanceCash))
					}
				}
			}
		yield user.id.toString
	}.provide(ZLayer.succeed(ds))

	override def update(account: Account): Task[Unit] = {
		for
			_ <- ctx.run {
				quote {
					query[AccountTable]
						.filter(p => p.id == lift(account.id))
						.updateValue(lift(AccountTable(account.id, account.balanceFood, account.balanceMeal, account.balanceCash)))
				}
			}
		yield ()
	}.provide(ZLayer.succeed(ds))

	override def delete(id: String): Task[Unit] = {
		for
			_ <- ctx.run {
				quote {
					query[AccountTable]
						.filter(p => p.id == lift(id))
						.delete
				}
			}
		yield ()
	}.provide(ZLayer.succeed(ds))


	override def updateFoodBalance(account: Account, balance: BigDecimal): Task[String]  =
		ctx
			.run {
				quote {
					query[AccountTable]
						.filter(p => p.id == lift(account.id))
						.update(_.balanceFood -> lift(balance))
						.returning(_.id)
				}
			}
			.provide(ZLayer.succeed(ds))
			.map(_.toString)

	override def updateMealBalance(account: Account, balance: BigDecimal): Task[Unit]  = {
		for
			_ <- ctx.run {
				quote {
						query[AccountTable]
							.filter(p => p.id == lift(account.id))
							.update(_.balanceMeal -> lift(balance))
					}
				}
		yield ()
	}.provide(ZLayer.succeed(ds))

	override def updateCashBalance(account: Account, balance: BigDecimal): Task[String]  =
		ctx
			.run {
				quote {
					query[AccountTable]
						.filter(p => p.id == lift(account.id))
						.update(_.balanceCash -> lift(balance))
						.returning(_.id)
				}
			}
			.provide(ZLayer.succeed(ds))
			.map(_.toString)

	override def lookup(id: String): Task[Option[Account]] =
		ctx.run {
				quote {
					query[AccountTable]
						.filter(p => p.id == lift(id))
						.map(u => Account(u.id, u.balanceFood, u.balanceMeal, u.balanceCash))
				}
			}
			.provide(ZLayer.succeed(ds))
			.map(_.headOption)

object PersistentAccountRepository:
	def layer: ZLayer[Any, Throwable, PersistentAccountRepository] =
		Quill.DataSource.fromPrefix("ConfigApp") >>>
			ZLayer.fromFunction(PersistentAccountRepository(_))
