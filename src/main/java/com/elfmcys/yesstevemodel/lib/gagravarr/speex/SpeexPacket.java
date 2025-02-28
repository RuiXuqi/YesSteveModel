/*
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
package com.elfmcys.yesstevemodel.lib.gagravarr.speex;

import com.elfmcys.yesstevemodel.lib.gagravarr.ogg.IOUtils;
import com.elfmcys.yesstevemodel.lib.gagravarr.ogg.OggStreamPacket;

/**
 * Parent of all Speex packets
 */
public interface SpeexPacket extends OggStreamPacket {
   public static final String MAGIC_HEADER_STR = "Speex   ";
   public static final byte[] MAGIC_HEADER_BYTES = IOUtils.toUTF8Bytes(MAGIC_HEADER_STR);
}