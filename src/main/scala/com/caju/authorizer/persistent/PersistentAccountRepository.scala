package com.caju.authorizer.persistent

import com.caju.authorizer.repository.*
import com.caju.authorizer.domain.Account
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

import javax.sql.DataSource

case class AccountTable(id: String, userId: String)

case class PersistentAccountRepository(ds: DataSource) extends AccountRepository:
	val ctx = new H2ZioJdbcContext(Escape)

	import ctx.*

	override def register(account: Account): Task[String] = {
		for
			_ <- ctx.run {
				quote {
					query[AccountTable].insertValue {
						lift(AccountTable(account.id, account.userId))
					}
				}
			}
		yield account.id.toString
	}.provide(ZLayer.succeed(ds))

	override def update(account: Account): Task[Unit] = {
		for
			_ <- ctx.run {
				quote {
					query[AccountTable]
						.filter(p => p.id == lift(account.id))
						.updateValue(lift(AccountTable(account.id, account.userId)))
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

	override def lookup(id: String): Task[Option[Account]] =
		ctx.run {
				quote {
					query[AccountTable]
						.filter(p => p.id == lift(id))
						.map(u => Account(u.id, u.userId))
				}
			}
			.provide(ZLayer.succeed(ds))
			.map(_.headOption)

	override def get(id: String): Task[Account] =
		ctx.run {
				quote {
					query[AccountTable]
						.filter(p => p.id == lift(id))
						.map(u => Account(u.id, u.userId))
				}
			}
			.provide(ZLayer.succeed(ds))
			.map(_.head)

object PersistentAccountRepository:
	def layer: ZLayer[Any, Throwable, PersistentAccountRepository] =
		Quill.DataSource.fromPrefix("ConfigApp") >>>
			ZLayer.fromFunction(PersistentAccountRepository(_))
