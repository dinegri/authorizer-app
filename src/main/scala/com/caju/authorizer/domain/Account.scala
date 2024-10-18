package com.caju.authorizer.domain

import zio.schema.{DeriveSchema, Schema}

case class Account(id: String, userId: String = "")

object Account:
	given Schema[Account] = DeriveSchema.gen[Account]
