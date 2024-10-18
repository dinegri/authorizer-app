package com.caju.authorizer

import com.caju.authorizer.persistent.{PersistentAccountBalanceRepository, PersistentAccountRepository, PersistentMccRepository, PersistentTransactionRepository}
import com.caju.authorizer.domain.{Account, Balance, AccountBalance, Transaction}
import com.caju.authorizer.repository.{AccountRepository, AccountBalanceRepository}
import com.caju.authorizer.routes.AuthorizerRoutes
import com.caju.authorizer.service.{AuthorizerService, AuthorizerServiceImpl}
import zio.*
import zio.http.*
import zio.http.netty.NettyConfig
import zio.http.netty.server.NettyDriver
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec
import zio.test.*

object AuthorizerRouteSpec extends ZIOSpecDefault:

	/*
			SELECT 5411, 'Food', 'PADARIA DO ZE               BAGE BR' UNION
			SELECT 5412, 'Food', 'UBER EATS                   SAO PAULO BR' UNION
			SELECT 5811, 'Meal', 'UBER EATS                   VILA VELA BR' UNION
			SELECT 5812, 'Meal', 'PADARIA DO ZE               SAO PAULO BR' UNION
			SELECT 1, 'Cash', 'UBER TRIP                   SAO PAULO BR'
	*/

	def fixture(accountId: String) = for {
		ar <- ZIO.service[AccountRepository]
		abr <- ZIO.service[AccountBalanceRepository]
		_ <- abr.delete(accountId)
		_ <- ar.delete(accountId)
		_ <- ar.register(Account(accountId, 100, 50, 101))
		_ <- abr.register(AccountBalance(s"1$accountId", accountId, Balance.Food.toString, 100.0))
		_ <- abr.register(AccountBalance(s"2$accountId", accountId, Balance.Meal.toString, 50.0))
		_ <- abr.register(AccountBalance(s"3$accountId", accountId, Balance.Cash.toString, 101.0))
	} yield ()

	override def spec: Spec[Any, Any] = suite("AuthorizerService")(
		test("Se o mcc for '5411', deve-se utilizar o saldo de FOOD") {
			for {
				client <- ZIO.service[Client]
				_ <- TestServer.addRoutes(AuthorizerRoutes())
				port <- ZIO.serviceWith[Server](_.port)
				url = URL.root.port(port)
				transaction = Transaction("1", 10.00, "5411", "PADARIA DO ZE               SAO PAULO BR")
				createResponse <- client(
					Request.post(url / "transactions", Body.from[Transaction](transaction))
				)
				result <- createResponse.body.asString
			} yield assertTrue(result == "{\"code\":\"00\"}")
		}.provideSome[Client with Driver with AuthorizerService](
			TestServer.layer,
			Scope.default,
			AuthorizerServiceImpl.layer,
			PersistentMccRepository.layer,
			PersistentAccountRepository.layer,
			PersistentAccountBalanceRepository.layer,
			PersistentTransactionRepository.layer
		) @@ TestAspect.before(
			fixture(accountId = "1")
		),

		test("Se o mcc for '5811', deve-se utilizar o saldo de MEAL") {
			for {
				client <- ZIO.service[Client]
				_ <- TestServer.addRoutes(AuthorizerRoutes())
				port <- ZIO.serviceWith[Server](_.port)
				url = URL.root.port(port)
				transaction = Transaction("2", 10, "5811", "PADARIA DO ZE               SAO PAULO BR")
				createResponse <- client(
					Request.post(url / "transactions", Body.from[Transaction](transaction))
				)
				result <- createResponse.body.asString
			} yield assertTrue(result == "{\"code\":\"00\"}")
		}.provideSome[Client with Driver with AuthorizerService](
			TestServer.layer,
			Scope.default,
			AuthorizerServiceImpl.layer,
			PersistentAccountRepository.layer,
			PersistentAccountBalanceRepository.layer,
			PersistentMccRepository.layer,
			PersistentTransactionRepository.layer
		) @@ TestAspect.before(
			fixture(accountId = "2")
		),

		test("Para quaisquer outros valores do mcc, deve-se utilizar o saldo de CASH") {
			for {
				client <- ZIO.service[Client]
				_ <- TestServer.addRoutes(AuthorizerRoutes())
				port <- ZIO.serviceWith[Server](_.port)
				url = URL.root.port(port)
				transaction = Transaction("3", 10, "5811", "UBER TRIP                   SAO PAULO BR")
				createResponse <- client(
					Request.post(url / "transactions", Body.from[Transaction](transaction))
				)
				result <- createResponse.body.asString
			} yield assertTrue(result == "{\"code\":\"00\"}")
		}.provideSome[Client with Driver with AuthorizerService](
			TestServer.layer,
			Scope.default,
			AuthorizerServiceImpl.layer,
			PersistentAccountRepository.layer,
			PersistentAccountBalanceRepository.layer,
			PersistentMccRepository.layer,
			PersistentTransactionRepository.layer
		) @@ TestAspect.before(
			fixture(accountId = "3")
		),

		test("Para quaisquer outros valores do mcc, deve-se rejeitar a transação quando saldo para cash não for suficiente") {
			for {
				client <- ZIO.service[Client]
				_ <- TestServer.addRoutes(AuthorizerRoutes())
				port <- ZIO.serviceWith[Server](_.port)
				url = URL.root.port(port)
				transaction = Transaction("4", 110, "5811", "UBER TRIP                   SAO PAULO BR")
				createResponse <- client(
					Request.post(url / "transactions", Body.from[Transaction](transaction))
				)
				result <- createResponse.body.asString
			} yield assertTrue(result == "{\"code\":\"51\"}")
		}.provideSome[Client with Driver with AuthorizerService](
			TestServer.layer,
			Scope.default,
			AuthorizerServiceImpl.layer,
			PersistentAccountRepository.layer,
			PersistentAccountBalanceRepository.layer,
			PersistentMccRepository.layer,
			PersistentTransactionRepository.layer
		) @@ TestAspect.before(
			fixture(accountId = "4")
		),

		test("Quando MCC estiver incorreto encontra a categoria correta pelo nome do comerciante") {
			for {
				client <- ZIO.service[Client]
				_ <- TestServer.addRoutes(AuthorizerRoutes())
				port <- ZIO.serviceWith[Server](_.port)
				url = URL.root.port(port)
				transaction = Transaction("5", 50, "5000", "UBER EATS                   VILA VELA BR")
				createResponse <- client(
					Request.post(url / "transactions", Body.from[Transaction](transaction))
				)
				result <- createResponse.body.asString
			} yield assertTrue(result == "{\"code\":\"00\"}")
		}.provideSome[Client with Driver with AuthorizerService](
			TestServer.layer,
			Scope.default,
			AuthorizerServiceImpl.layer,
			PersistentAccountRepository.layer,
			PersistentAccountBalanceRepository.layer,
			PersistentMccRepository.layer,
			PersistentTransactionRepository.layer
		) @@ TestAspect.before(
			fixture(accountId = "5")
		),

		test("Quando MCC estiver incorreto encontra a categoria correta pelo nome do comerciante e rejeito devido saldo insuficiente") {
			for {
				client <- ZIO.service[Client]
				_ <- TestServer.addRoutes(AuthorizerRoutes())
				port <- ZIO.serviceWith[Server](_.port)
				url = URL.root.port(port)
				transaction = Transaction("6", 102, "5000", "UBER EATS                   VILA VELA BR")
				createResponse <- client(
					Request.post(url / "transactions", Body.from[Transaction](transaction))
				)
				result <- createResponse.body.asString
			} yield assertTrue(result == "{\"code\":\"51\"}")
		}.provideSome[Client with Driver with AuthorizerService](
			TestServer.layer,
			Scope.default,
			AuthorizerServiceImpl.layer,
			PersistentAccountRepository.layer,
			PersistentAccountBalanceRepository.layer,
			PersistentMccRepository.layer,
			PersistentTransactionRepository.layer
		) @@ TestAspect.before(
			fixture(accountId = "6")
		)
	).provide(
		ZLayer.succeed(Server.Config.default.onAnyOpenPort),
		Client.default,
		NettyDriver.customized,
		ZLayer.succeed(NettyConfig.defaultWithFastShutdown),
		AuthorizerServiceImpl.layer,
		PersistentMccRepository.layer,
		PersistentAccountRepository.layer,
		PersistentAccountBalanceRepository.layer,
		PersistentTransactionRepository.layer
	).provideLayerShared(PersistentAccountRepository.layer)

	override def aspects: Chunk[TestAspectPoly] =
		Chunk(TestAspect.timeout(60.seconds), TestAspect.timed)
