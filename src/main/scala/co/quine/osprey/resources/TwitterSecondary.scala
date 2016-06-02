package co.quine.osprey
package resources

sealed trait TwitterSecondary {
  val id: Option[Long]
  val screenName: Option[String]
}
final case class UserHashtags(id: Option[Long] = None, screenName: Option[String] = None) extends TwitterSecondary
final case class UserMentions(id: Option[Long] = None, screenName: Option[String] = None) extends TwitterSecondary
final case class UserUrls(id: Option[Long] = None,
                          screenName: Option[String] = None,
                          filterDomain: Option[String] = None) extends TwitterSecondary
final case class UserImages(id: Option[Long] = None,
                            screenName: Option[String] = None,
                            filterDomain: Option[String] = None) extends TwitterSecondary
