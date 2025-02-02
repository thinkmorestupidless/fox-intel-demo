/*
 * Copyright (C) 2016-2020 Lightbend Inc. <https://www.lightbend.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.cloudflow

import carly.data.CallRecord
import com.example.data.SyncMailbox
import spray.json._

case object JsonCallRecord extends DefaultJsonProtocol {
  implicit val crFormat = jsonFormat(CallRecord.apply, "user", "other", "direction", "duration", "timestamp")
  implicit val smFormat = jsonFormat(SyncMailbox.apply, "user", "email", "direction", "duration", "timestamp")
}
