package com.caju.authorizer.repository

import zio.*
import com.caju.authorizer.domain.{AccountBalance, AccountBalanceType, Balance}

trait AccountBalanceRepository:
  def register(account: AccountBalance): Task[String]

  def lookup(id: String): Task[Option[AccountBalance]]

  def update(account: AccountBalance): Task[Unit]

  def delete(accountId: String): Task[Unit]

  def balances(accountId: String): Task[List[AccountBalance]]

  def balancesMap(accountId: String): Task[Map[Balance, AccountBalanceType]]

object AccountBalanceRepository:
  def register(account: AccountBalance): ZIO[AccountBalanceRepository, Throwable, String] =
    ZIO.serviceWithZIO[AccountBalanceRepository](_.register(account))

  def lookup(id: String): ZIO[AccountBalanceRepository, Throwable, Option[AccountBalance]] =
    ZIO.serviceWithZIO[AccountBalanceRepository](_.lookup(id))

  def update(account: AccountBalance): ZIO[AccountBalanceRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[AccountBalanceRepository](_.update(account))

  def delete(accountId: String): ZIO[AccountBalanceRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[AccountBalanceRepository](_.delete(accountId))

  def balances(accountId: String): ZIO[AccountBalanceRepository, Throwable, List[AccountBalance]] =
    ZIO.serviceWithZIO[AccountBalanceRepository](_.balances(accountId))

  def balancesMap(accountId: String): ZIO[AccountBalanceRepository, Throwable, Map[Balance, AccountBalanceType]] =
    ZIO.serviceWithZIO[AccountBalanceRepository](_.balancesMap(accountId))

