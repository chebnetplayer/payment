package misis.paymentSystem.model

case class Cashback(category: String, cashback: Int)
case class CreateCashback(category: String, cashback: Int)
case class ChangeCashback(category: String, cashback: Int)