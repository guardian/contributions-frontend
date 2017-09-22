package utils

import scala.reflect.ClassTag

trait RuntimeClassUtils {

  def runtimeClass[A : ClassTag]: Class[_] = reflect.classTag[A].runtimeClass
}
