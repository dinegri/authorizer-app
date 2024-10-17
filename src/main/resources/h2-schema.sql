CREATE TABLE IF NOT EXISTS "TransactionTable"(
    "uuid" uuid NOT NULL PRIMARY KEY,
	"account" VARCHAR(255),
	"totalAmount" int,
	"mcc" VARCHAR(255),
	"merchant" VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS "AccountTable"(
	"id" INT NOT NULL PRIMARY KEY,
	"balanceFood" INT,
	"balanceMeal" INT,
	"balanceCash" INT
);

CREATE TABLE IF NOT EXISTS "MccTable"(
								 "code" VARCHAR(255) NOT NULL PRIMARY KEY,
								 "balanceType" VARCHAR(255),
								 "merchant" VARCHAR(255)
);

INSERT INTO "AccountTable" SELECT * FROM (
										   SELECT 1, 100, 50, 101
									   ) x WHERE NOT EXISTS(SELECT * FROM "AccountTable");

INSERT INTO "MccTable" SELECT * FROM (
								 SELECT 5411, 'Food', 'PADARIA DO ZE               SAO PAULO BR' UNION
								 SELECT 5412, 'Food', 'PADARIA DO ZE               SAO PAULO BR' UNION
								 SELECT 5811, 'Meal', 'PADARIA DO ZE               SAO PAULO BR' UNION
								 SELECT 5812, 'Meal', 'PADARIA DO ZE               SAO PAULO BR' UNION
							     SELECT 1, 'Cash', 'UBER TRIP                   SAO PAULO BR '
							) x WHERE NOT exists(SELECT * FROM "MccTable");
