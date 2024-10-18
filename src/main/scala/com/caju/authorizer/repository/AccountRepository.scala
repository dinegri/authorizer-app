package com.caju.authorizer.repository

import zio.*
import zio.schema.*
import com.caju.authorizer.domain.Account

trait AccountRepository:
  def register(user: Account): Task[String]

  def lookup(id: String): Task[Option[Account]]

  def get(id: String): Task[Account]

  def update(account: Account): Task[Unit]

  def delete(id: String): Task[Unit]

object AccountRepository:
  def register(account: Account): ZIO[AccountRepository, Throwable, String] =
    ZIO.serviceWithZIO[AccountRepository](_.register(account))

  def lookup(id: String): ZIO[AccountRepository, Throwable, Option[Account]] =
    ZIO.serviceWithZIO[AccountRepository](_.lookup(id))

  def get(id: String): ZIO[AccountRepository, Throwable, Account] =
    ZIO.serviceWithZIO[AccountRepository](_.get(id))

  def update(account: Account): ZIO[AccountRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[AccountRepository](_.update(account))

  def delete(id: String): ZIO[AccountRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[AccountRepository](_.delete(id))
