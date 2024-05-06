package misis.paymentSystem.repository

import misis.paymentSystem.model._

import java.util.UUID
import scala.concurrent.Future

trait AccountCashbackRepository {
  def list(): Future[Seq[Account]]
  def createAccount(acc: CreateAccount): Future[Account]
  def get(acc: UUID): Future[Account]
  def getAccount(acc: GetAccount): Future[Seq[UUID]]
  def refillAccount(acc: RefillAccount): Future[Account]
  def withdrawalMoney(acc: WithdrawalMoney): Future[Account]
  def changeCash(acc: ChangeCash): Future[Int]
  def getCashbacks(): Future[Seq[Cashback]]
  def createCashback(opt: CreateCashback): Future[Cashback]
  def changeCashback(opt: ChangeCashback): Future[Option[Cashback]]
  def getPercentOfCashback(opt: String): Future[Int]
  def foreignChange(acc: ChangeCash): Future[Account]
}
