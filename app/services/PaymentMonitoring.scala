package services

import javax.inject.{Inject, Singleton}

import dao.PaymentTokenDAO

@Singleton
class PaymentMonitoring @Inject ()(paymentTokenDao: PaymentTokenDAO) {

  def monitor(): Unit = {
    paymentTokenDao.updatePayments()
  }

}
