package com.caju.authorizer.service

import com.caju.authorizer.repository.*
import com.caju.authorizer.domain.Balance.{Cash, Food, Meal}
import com.caju.authorizer.domain.{AccountBalance, AccountBalanceType, Balance, MccCash, MccCode, Transaction}
import com.caju.authorizer.service.AuthorizationStatus.*
import zio.*

enum AuthorizationStatus(code: String) {
	case Authorized extends AuthorizationStatus("00")
	case Rejected extends AuthorizationStatus("51")
	case Unexpected extends AuthorizationStatus("07")

	override def toString: String = code
}

case class AuthorizationCode(code: String)

trait AuthorizerService {
	def authorize(transaction: Transaction): Task[AuthorizationCode]
}

object AuthorizerService {
	def authorize(transaction: Transaction): ZIO[AuthorizerService, Throwable, AuthorizationCode] =
		ZIO.serviceWithZIO[AuthorizerService](_.authorize(transaction))
}

case class AuthorizerServiceImpl(accountRepository: AccountRepository,
		accountBalanceRepository: AccountBalanceRepository,
		mccRepository: MccRepository,
		transactionRepository: TransactionRepository) extends AuthorizerService {

	override def authorize(transaction: Transaction): Task[AuthorizationCode] =
		for {
			account  <- accountRepository.get(transaction.account).absorb
			mcc      <- mccRepository.lookupByMerchantName(transaction.merchant).flatMap(ZIO.fromOption).foldCauseZIO(_ => ZIO.succeed(new MccCash()), data => ZIO.succeed(data))
			balances <- accountBalanceRepository.balancesMap(transaction.account)
			update   <- debit(transaction, balances, MccCode(mcc.code, Balance.valueOf(mcc.balanceType), mcc.merchant))
			_        <- accountBalanceRepository.update(update._1)
		} yield update._2


	private def debit(transaction: Transaction, balances: Map[Balance, AccountBalanceType], mcc: MccCode): Task[(AccountBalance, AuthorizationCode)] =
		def debitFallback(b: Balance, c: Balance) =
			if (hasBalance(balances(b).balance, transaction.totalAmount))
				debitBalance(balances(b), transaction, b)
			else if (hasBalance(balances(c).balance, transaction.totalAmount))
				debitBalance(balances(c), transaction, c)
			else noBalance(balances(mcc.balanceType))

		mcc.balanceType match
			case Food => debitFallback(Food, Cash)
			case Meal => debitFallback(Meal, Cash)
			case _ => debitFallback(Cash, Cash)

	private def noBalance(balance: AccountBalanceType): Task[(AccountBalance, AuthorizationCode)] =
		ZIO.succeed(AccountBalance(balance.id, balance.accountId, balance.balanceType.toString, balance.balance),
			AuthorizationCode(Rejected.toString))

	private def debitBalance(account: AccountBalanceType, transaction: Transaction, balance: Balance): Task[(AccountBalance, AuthorizationCode)] = {
		ZIO.succeed(AccountBalance(account.id, account.accountId, account.balanceType.toString, account.balance - transaction.totalAmount), AuthorizationCode(Authorized.toString))
	}

	private def hasBalance(balance: BigDecimal, amount: BigDecimal): Boolean = balance >= amount
}

object AuthorizerServiceImpl {
	val layer: ZLayer[AccountRepository with AccountBalanceRepository with MccRepository with TransactionRepository, Nothing, AuthorizerService] =
		ZLayer {
			for {
				accountRepository         <- ZIO.service[AccountRepository]
				accountBalanceRepository  <- ZIO.service[AccountBalanceRepository]
				merchantCategoryCodes     <- ZIO.service[MccRepository]
				transactionRepository     <- ZIO.service[TransactionRepository]
			} yield AuthorizerServiceImpl(accountRepository, accountBalanceRepository, merchantCategoryCodes, transactionRepository)
		}
}


