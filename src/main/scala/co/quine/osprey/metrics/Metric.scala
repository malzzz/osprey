package co.quine.osprey
package metrics

sealed trait Metric
case class OperationMetric(uuid: String, resource: String, timeMillis: Long) extends Metric
case class ResponseMetric(uuid: String, service: String, records: Int, timeMillis: Long) extends Metric