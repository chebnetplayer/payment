package misis.paymentSystem.db

import misis.paymentSystem.model.Cashback
import slick.jdbc.PostgresProfile.api._

object CashbackDb {
  class  CashbackTable(tag: Tag) extends Table[Cashback](tag, "cashbacks") {
    val category = column[String]("category", O.PrimaryKey)
    val cashback = column[Int]("cashback")

    def * = (category, cashback) <> ((Cashback.apply _).tupled, Cashback.unapply _)
  }

  val cashbackTable = TableQuery[CashbackTable]
}
