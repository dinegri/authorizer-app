CREATE TABLE IF NOT EXISTS "TransactionTable"(
	"id" INT PRIMARY KEY AUTO_INCREMENT,
	"account" VARCHAR(255),
	"totalAmount" NUMERIC(20, 2),
	"mcc" VARCHAR(255),
	"merchant" VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS "AccountTable"(
	"id" VARCHAR(255) NOT NULL PRIMARY KEY,
	"balanceFood" NUMERIC(20, 2),
	"balanceMeal" NUMERIC(20, 2),
	"balanceCash" NUMERIC(20, 2)
);

CREATE TABLE IF NOT EXISTS "AccountBalanceTable"(
	"id" VARCHAR(255) NOT NULL PRIMARY KEY,
	"accountId" VARCHAR(255),
	"balanceType" VARCHAR(20),
	"balance" NUMERIC(20, 2)
);

CREATE TABLE IF NOT EXISTS "MccTable"(
	"code" VARCHAR(255) NOT NULL PRIMARY KEY,
	"balanceType" VARCHAR(255),
	"merchant" VARCHAR(255)
);

INSERT INTO "MccTable" SELECT * FROM (
	 SELECT 5411, 'Food', 'PADARIA DO ZE               BAGE BR' UNION
	 SELECT 5412, 'Food', 'UBER EATS                   SAO PAULO BR' UNION
	 SELECT 5811, 'Meal', 'UBER EATS                   VILA VELA BR' UNION
	 SELECT 5812, 'Meal', 'PADARIA DO ZE               SAO PAULO BR' UNION
	 SELECT 1, 'Cash', 'UBER TRIP                   SAO PAULO BR'
 ) x WHERE NOT EXISTS(SELECT * FROM "MccTable");
/*
INSERT INTO "AccountBalanceTable" SELECT * FROM (
	 SELECT 1, '1', 'Food', 100.0 UNION
	 SELECT 2, '1', 'Meal', 50.0 UNION
	 SELECT 3, '1', 'Cash', 50.0
 ) x WHERE NOT EXISTS(SELECT * FROM "AccountBalanceTable");
 */

