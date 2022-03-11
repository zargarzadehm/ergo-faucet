package services

import dao.PaymentTokenDAO
import utils.{Conf, Discord, Explorer}

import javax.inject.{Inject, Singleton}

@Singleton
class PaymentMonitoring @Inject ()(paymentTokenDao: PaymentTokenDAO) {

  def monitor(): Unit = {
    paymentTokenDao.updatePayments()
  }

}
