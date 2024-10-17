package com.caju.authorizer

import com.caju.authorizer.database.{PersistentAccountRepository, PersistentMccRepository, PersistentTransactionRepository}
import com.caju.authorizer.repository.Transaction
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
			SELECT 1, 100 Food, 50 Meal, 101 Cash
			SELECT 5411, 'Food', 'PADARIA DO ZE               BAGE BR' UNION
			SELECT 5412, 'Food', 'UBER EATS                   SAO PAULO BR' UNION
			SELECT 5811, 'Meal', 'UBER EATS                   VILA VELA BR' UNION
			SELECT 5812, 'Meal', 'PADARIA DO ZE               SAO PAULO BR' UNION
			SELECT 1, 'Cash', 'UBER TRIP                   SAO PAULO BR '
	*/

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
			PersistentTransactionRepository.layer
		),
		test("Se o mcc for '5811', deve-se utilizar o saldo de MEAL") {
			for {
				client <- ZIO.service[Client]
				_ <- TestServer.addRoutes(AuthorizerRoutes())
				port <- ZIO.serviceWith[Server](_.port)
				url = URL.root.port(port)
				transaction = Transaction("1", 10, "5811", "PADARIA DO ZE               SAO PAULO BR")
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
			PersistentMccRepository.layer,
			PersistentTransactionRepository.layer
		),
		test("Para quaisquer outros valores do mcc, deve-se utilizar o saldo de CASH") {
			for {
				client <- ZIO.service[Client]
				_ <- TestServer.addRoutes(AuthorizerRoutes())
				port <- ZIO.serviceWith[Server](_.port)
				url = URL.root.port(port)
				transaction = Transaction("1", 10, "5811", "UBER TRIP                   SAO PAULO BR")
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
			PersistentMccRepository.layer,
			PersistentTransactionRepository.layer
		),
		test("Para quaisquer outros valores do mcc, deve-se rejeitar a transação quando saldo para cash não for suficiente") {
			for {
				client <- ZIO.service[Client]
				_ <- TestServer.addRoutes(AuthorizerRoutes())
				port <- ZIO.serviceWith[Server](_.port)
				url = URL.root.port(port)
				transaction = Transaction("1", 110, "5811", "UBER TRIP                   SAO PAULO BR")
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
			PersistentMccRepository.layer,
			PersistentTransactionRepository.layer
		),
		test("Debita do Cash quando MCC não for encontrado") {
			for {
				client <- ZIO.service[Client]
				_ <- TestServer.addRoutes(AuthorizerRoutes())
				port <- ZIO.serviceWith[Server](_.port)
				url = URL.root.port(port)
				transaction = Transaction("1", 10, "5000", "UBER TRIP                   SAO PAULO BR")
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
			PersistentMccRepository.layer,
			PersistentTransactionRepository.layer
		)
	).provide(
		ZLayer.succeed(Server.Config.default.onAnyOpenPort),
		Client.default,
		NettyDriver.customized,
		ZLayer.succeed(NettyConfig.defaultWithFastShutdown),
		AuthorizerServiceImpl.layer,
		PersistentMccRepository.layer,
		PersistentAccountRepository.layer,
		PersistentTransactionRepository.layer
	)

	override def aspects: Chunk[TestAspectPoly] =
		Chunk(TestAspect.timeout(60.seconds), TestAspect.timed)
