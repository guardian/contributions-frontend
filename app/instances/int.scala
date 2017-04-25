package instances

import java.text.NumberFormat
import java.util.Locale

import cats.Show

trait IntInstances {

  private val numberFormatter = NumberFormat.getInstance(Locale.UK)

  // Used to format numbers on the client side.
  implicit val integerShow: Show[Int] = Show.show(integer => numberFormatter.format(integer))
}
