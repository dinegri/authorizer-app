package com.caju.authorizer.service

import com.caju.authorizer.repository.*
import com.caju.authorizer.repository.Balance.{Food, Meal}
import zio.*
import AuthorizationStatus._

case class CustomResponse()

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

case class AuthorizerServiceImpl(
																	accountRepository: AccountRepository,
																	mccRepository: MccRepository,
																	transactionRepository: TransactionRepository) extends AuthorizerService {

	override def authorize(transaction: Transaction): Task[AuthorizationCode] =
		for {
			_        <- transactionRepository.register(transaction)
			account  <- accountRepository.lookup(transaction.account)
			fallback <- mccRepository.lookupByMerchantName(transaction.merchant).flatMap {
				case mcc@Some(_) => {
					ZIO.succeed(mcc)
				}
				case None => {
					mccRepository.lookup(transaction.mcc)
				}
			}
			mcc      <- ZIO.succeed(fallback.getOrElse(new MccCash()))
			code     <- debit(transaction, account.get, MccCode(mcc.code, Balance.valueOf(mcc.balanceType), mcc.merchant))
		} yield code

	private def debit(transaction: Transaction, account: Account, mcc: MccCode): Task[AuthorizationCode] =
		mcc.balanceType match
			case Food => {
				if (hasBalance(account.balanceFood, transaction.totalAmount))
					debitBalance(account, transaction, Balance.Food)
				else if (hasBalance(transaction.totalAmount, account.balanceCash))
					debitBalance(account, transaction, Balance.Cash)
				else noBalance
			}
			case Meal => {
				if (hasBalance(account.balanceMeal, transaction.totalAmount))
					debitBalance(account, transaction, Balance.Meal)
				else if (hasBalance(account.balanceCash, transaction.totalAmount))
					debitBalance(account, transaction, Balance.Cash)
				else noBalance
			}
			case _ => {
				if (hasBalance(account.balanceCash, transaction.totalAmount))
					debitBalance(account, transaction, Balance.Cash)
				else noBalance
			}

	private def hasBalance(balance: Long, amount: Long): Boolean = balance >= amount

	private def noBalance: Task[AuthorizationCode]  = ZIO.succeed(AuthorizationCode(Rejected.toString))

	private def debitBalance(account: Account, transaction: Transaction, balance: Balance): Task[AuthorizationCode] = {
		val acc = balance match
			case Balance.Food => accountRepository.updateFoodBalance(account,  account.balanceFood - transaction.totalAmount)
			case Balance.Meal => accountRepository.updateMealBalance(account,  account.balanceMeal - transaction.totalAmount)
			case Balance.Cash => accountRepository.updateCashBalance(account,  account.balanceCash - transaction.totalAmount)

		ZIO.succeed(AuthorizationCode(Authorized.toString))
	}
}

object AuthorizerServiceImpl {
	val layer: ZLayer[AccountRepository with MccRepository with TransactionRepository, Nothing, AuthorizerService] =
		ZLayer {
			for {
				accountRepo           <- ZIO.service[AccountRepository]
				merchantCategoryCodes <- ZIO.service[MccRepository]
				transactionRepository <- ZIO.service[TransactionRepository]
			} yield AuthorizerServiceImpl(accountRepo, merchantCategoryCodes, transactionRepository)
		}
}


