package com.caju.authorizer.domain

enum Balance:
	case Food, Meal, Cash

case class Mcc(code: String = "", balanceType: String = "Cash", merchant: String = "")

type MccCash = Mcc

case class MccCode(code: String, balanceType: Balance, merchant: String)
