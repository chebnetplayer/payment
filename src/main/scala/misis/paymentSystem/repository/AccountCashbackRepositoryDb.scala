package misis.paymentSystem.repository

import misis.paymentSystem.db.AccountDb._
import misis.paymentSystem.db.CashbackDb.cashbackTable
import misis.paymentSystem.model.{Account, Cashback, ChangeCash, ChangeCashback, ChangeRequest, CreateAccount, CreateCashback, GetAccount, RefillAccount, WithdrawalMoney}
import slick.jdbc.PostgresProfile.api._

import java.lang.Math.round
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class AccountCashbackRepositoryDb(client: PaymentClient)(implicit val ec: ExecutionContext, db: Database) extends AccountCashbackRepository {
  override def list(): Future[Seq[Account]] = {
    db.run(accountTable.result)
  }

  override def createAccount(acc: CreateAccount): Future[Account] = {
    val a = Account(owner = acc.owner, email = acc.email, count = acc.count)
    for {
      _ <- db.run(accountTable += a)
      res <- get(a.id)
    } yield res
  }

  override def get(acc: UUID): Future[Account] = {
    val query = accountTable.filter(_.id === acc)
    db.run(query.result.head)
  }

  def find(acc: UUID): Future[Option[Account]] = {
    db.run(accountTable.filter(_.id === acc).result.headOption)
  }

  override def getAccount(acc: GetAccount): Future[Seq[UUID]] = {
    val query = accountTable.filter(_.email === acc.email)
    db.run(query.map(a => a.id).result).map { a =>
      if (a.nonEmpty) a
      else throw new NonAccount
    }
  }

  override def refillAccount(acc: RefillAccount): Future[Account] = {
    db.run(accountTable.filter(_.id === acc.id).map(a => a.id).result).flatMap { a =>
      if (a.nonEmpty) {
        get(acc.id).map { account =>
          val new_count = account.count + acc.amount
          db.run(accountTable.filter(_.id === acc.id).map(_.count).update(new_count))
          account
        }
      }
      else throw new NonAccount
    }
  }

  override def withdrawalMoney(acc: WithdrawalMoney): Future[Account] = {
    db.run(accountTable.filter(_.id === acc.id).map(a => a.id).result).flatMap { a =>
      if (a.nonEmpty) {
        get(acc.id).map { ac =>
          val new_count = ac.count - acc.amount
          if (new_count > 0) {
            db.run(accountTable.filter(_.id === acc.id).map(_.count).update(new_count))
            ac
          }
          else
            throw new TooMuchWithdrawalAmount
        }
      }
      else throw new NonAccount
    }
  }


  override def getCashbacks(): Future[Seq[Cashback]] = db.run(cashbackTable.result)

  override def createCashback(opt: CreateCashback): Future[Cashback] = {
    val cb = Cashback(category = opt.category, cashback = opt.cashback)
    for {
      _ <- db.run(cashbackTable += cb)
         res <- db.run(cashbackTable.filter(_.category === cb.category).result.head)
    } yield res
  }

  def findCashback(opt: String): Future[Option[Cashback]] = {
    db.run(cashbackTable.filter(_.category === opt).result.headOption)
  }

  override def changeCashback(opt: ChangeCashback): Future[Option[Cashback]] = {
    for {
      _ <- db.run(cashbackTable
      .filter(_.category === opt.category)
      .map(cb => cb.cashback).result).map {cb =>
      cb.nonEmpty match {
      case true => cb.head
      case false => throw new CashbackNonExist
    }
      }
      _ <- db.run {
      cashbackTable
      .filter (_.category === opt.category)
      .map (_.cashback)
      .update (opt.cashback)
    }
      res <- findCashback(opt.category)
    } yield res
  }

  override def getPercentOfCashback(opt: String): Future[Int] = {
    db.run(cashbackTable.filter(_.category === opt).map(cb => cb.cashback).result).map { cb =>
      cb.nonEmpty match {
        case true => cb.head
        case false => throw new CashbackNonExist
      }
    }
  }

  override def changeCash(acc: ChangeCash): Future[Int] = {
      db.run(accountTable.filter(_.id === acc.from).map(_.id).result).flatMap { ac =>
        ac.nonEmpty match {
          case true => db.run(accountTable.filter(_.id === acc.to).map(_.id).result).flatMap { ac =>
            ac.nonEmpty match {
              case true => get(acc.from).flatMap { account =>
                (account.count - acc.amount) >= 0 match {
                  case true => get(acc.from).map { account =>
                    val new_vol = account.count - acc.amount
                    acc.category.nonEmpty match {
                      case true => getPercentOfCashback(acc.category.get).map { perc =>
                        val newest_vol = new_vol + round(acc.amount / 100 * perc)
                        for {
                          _ <- db.run(accountTable.filter(_.id === acc.from).map(_.count).update(newest_vol))
                          res <- find(acc.from)
                        } yield res
                      }
                      case false => for {
                        _ <- db.run(accountTable.filter(_.id === acc.from).map(_.count).update(new_vol))
                        res <- find(acc.from)
                      } yield res
                    }
                    get(acc.to).flatMap { account =>
                      val new_count = account.count + acc.amount
                      db.run(accountTable.filter(_.id === acc.to).map(_.count).update(new_count))
                    }
                    acc.amount
                  }
                  case false => throw new TooMuchWithdrawalAmount
                }
              }
              case false => foreignChange(acc).map(a => a.count)
            }
          }
          case false => throw new NonAccount
        }
      }
    }

    override def foreignChange(acc: ChangeCash): Future[Account] = {
      get(acc.from).flatMap { account =>
        (account.count - acc.amount) >= 0 match {
          case true => get(acc.from).map { account =>
            val new_vol = account.count - acc.amount
            acc.category.nonEmpty match {
              case true => getPercentOfCashback(acc.category.get).map { perc =>
                val newest_vol = new_vol + round(acc.amount / 100 * perc)
                for {
                  _ <- db.run(accountTable.filter(_.id === acc.from).map(_.count).update(newest_vol))
                  res <- find(acc.from)
                } yield res
              }
              case false => for {
                _ <- db.run(accountTable.filter(_.id === acc.from).map(_.count).update(new_vol))
                res <- find(acc.from)
              } yield res
            }
            acc.amount
          }
          case false => throw new TooMuchWithdrawalAmount
        }
      }.flatMap(_ => client.payment(ChangeRequest(acc.to, acc.amount)))
    }
  }

case class NonAccount() extends Exception
case class TooMuchWithdrawalAmount() extends Exception
case class CashbackNonExist() extends Exception