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
			account  <- accountRepository.lookup(transaction.account)
			mcc      <- mccRepository.lookupByMerchantName(transaction.merchant).flatMap(ZIO.fromOption).foldZIO(_ => ZIO.succeed(new MccCash()), data => ZIO.succeed(data))
			balances <- accountBalanceRepository.balancesMap(transaction.account)
			update   <- debit(transaction, balances, MccCode(mcc.code, Balance.valueOf(mcc.balanceType), mcc.merchant))
			_        <- accountBalanceRepository.update(update._1)
		} yield update._2

	private def debit(transaction: Transaction, balances: Map[Balance, AccountBalanceType], mcc: MccCode): Task[(AccountBalance, AuthorizationCode)] =
		mcc.balanceType match
			case Food => {
				if (hasBalance(balances(Food).balance, transaction.totalAmount))
					debitBalance(balances(Food), transaction, Balance.Food)
				else if (hasBalance(transaction.totalAmount, balances(Cash).balance))
					debitBalance(balances(Cash), transaction, Balance.Cash)
				else noBalance(balances(mcc.balanceType))
			}
			case Meal => {
				if (hasBalance(balances(Meal).balance, transaction.totalAmount))
					debitBalance(balances(Meal), transaction, Balance.Meal)
				else if (hasBalance(balances(Cash).balance, transaction.totalAmount))
					debitBalance(balances(Cash), transaction, Balance.Cash)
				else noBalance(balances(mcc.balanceType))
			}
			case _ => {
				if (hasBalance(balances(Cash).balance, transaction.totalAmount))
					debitBalance(balances(Cash), transaction, Balance.Cash)
				else noBalance(balances(mcc.balanceType))
			}

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


