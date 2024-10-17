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
			update   <- debit(transaction, account.get, MccCode(mcc.code, Balance.valueOf(mcc.balanceType), mcc.merchant))
			_        <- accountRepository.update(update._1)
		} yield update._2

	private def debit(transaction: Transaction, account: Account, mcc: MccCode): Task[(Account, AuthorizationCode)] =
		mcc.balanceType match
			case Food => {
				if (hasBalance(account.balanceFood, transaction.totalAmount))
					debitBalance(account, transaction, Balance.Food)
				else if (hasBalance(transaction.totalAmount, account.balanceCash))
					debitBalance(account, transaction, Balance.Cash)
				else noBalance(account)
			}
			case Meal => {
				if (hasBalance(account.balanceMeal, transaction.totalAmount))
					debitBalance(account, transaction, Balance.Meal)
				else if (hasBalance(account.balanceCash, transaction.totalAmount))
					debitBalance(account, transaction, Balance.Cash)
				else noBalance(account)
			}
			case _ => {
				if (hasBalance(account.balanceCash, transaction.totalAmount))
					debitBalance(account, transaction, Balance.Cash)
				else noBalance(account)
			}

	private def hasBalance(balance: BigDecimal, amount: BigDecimal): Boolean = balance >= amount

	private def noBalance(account: Account): Task[(Account, AuthorizationCode)]  = ZIO.succeed(account, AuthorizationCode(Rejected.toString))

	private def debitBalance(account: Account, transaction: Transaction, balance: Balance): Task[(Account, AuthorizationCode)] = {
		val acc = balance match
			case Balance.Food => Account(account.id, account.balanceFood - transaction.totalAmount, account.balanceMeal, account.balanceCash)
			case Balance.Meal => Account(account.id, account.balanceFood, account.balanceMeal - transaction.totalAmount, account.balanceCash)
			case Balance.Cash => Account(account.id, account.balanceFood, account.balanceMeal, account.balanceCash - transaction.totalAmount)

		ZIO.succeed(acc, AuthorizationCode(Authorized.toString))
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


