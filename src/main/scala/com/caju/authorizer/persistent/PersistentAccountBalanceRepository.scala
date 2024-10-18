package com.caju.authorizer.persistent

import com.caju.authorizer.repository.*
import com.caju.authorizer.domain.{Account, AccountBalance, Balance, AccountBalanceType}
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

import javax.sql.DataSource

case class AccountBalanceTable(id: String, accountId: String, balanceType: String, balance: BigDecimal)

case class PersistentAccountBalanceRepository(ds: DataSource) extends AccountBalanceRepository:
	val ctx = new H2ZioJdbcContext(Escape)

	import ctx.*

	override def register(balance: AccountBalance): Task[String] = {
		for
			_ <- ctx.run {
				quote {
					query[AccountBalanceTable].insertValue {
						lift(AccountBalanceTable(balance.id, balance.accountId, balance.balanceType, balance.balance))
					}
				}
			}
		yield balance.id.toString
	}.provide(ZLayer.succeed(ds))

	override def update(account: AccountBalance): Task[Unit] = {
		for
			_ <- ctx.run {
				quote {
					query[AccountBalanceTable]
						.filter(p => p.id == lift(account.id))
						.updateValue(lift(AccountBalanceTable(account.id, account.accountId, account.balanceType, account.balance)))
				}
			}
		yield ()
	}.provide(ZLayer.succeed(ds))

	override def delete(accountId: String): Task[Unit] = {
		for
			_ <- ctx.run {
				quote {
					query[AccountBalanceTable]
						.filter(p => p.accountId == lift(accountId))
						.delete
				}
			}
		yield ()
	}.provide(ZLayer.succeed(ds))


	override def lookup(id: String): Task[Option[AccountBalance]] =
		ctx.run {
				quote {
					query[AccountBalanceTable]
						.filter(p => p.id == lift(id))
						.map(u => AccountBalance(u.id, u.accountId, u.balanceType, u.balance))
				}
			}
			.provide(ZLayer.succeed(ds))
			.map(_.headOption)

	override def balances(accountId: String): Task[List[AccountBalance]] =
		ctx
			.run {
				quote {
					query[AccountBalanceTable].map(u => AccountBalance(u.id, u.accountId, u.balanceType, u.balance))
				}
			}
			.provide(ZLayer.succeed(ds))

	override def balancesMap(accountId: String): Task[Map[Balance, AccountBalanceType]] =
		ctx
			.run {
				quote {
					query[AccountBalanceTable].map(u => AccountBalance(u.id, u.accountId, u.balanceType, u.balance))
				}
			}
			.provide(ZLayer.succeed(ds))
			.map(_.groupBy(t => Balance.valueOf(t.balanceType)) map { case (p, ts) =>  p -> 
				AccountBalanceType(ts.head.id, ts.head.accountId, Balance.valueOf(ts.head.balanceType), ts.head.balance ) })


object PersistentAccountBalanceRepository:
	def layer: ZLayer[Any, Throwable, PersistentAccountBalanceRepository] =
		Quill.DataSource.fromPrefix("ConfigApp") >>>
			ZLayer.fromFunction(PersistentAccountBalanceRepository(_))
