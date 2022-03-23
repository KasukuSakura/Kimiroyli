# Kimiroyli

Kimiroyli 是一个致力于 JVM 安全的安全管理框架, 致力于保护整个系统不受恶意外部代码攻击

----

## Features

- 系统安全
  - 文件 IO 限制
  - 网络访问控制
  - JNI 链接限制
- Java Runtime 安全
  - 反射控制
    - java.lang.reflect.*
    - java.lang.invoke.*
  - `System.exit()` 拦截
  - 权限上下文 api
  - `sun.misc.Unsafe` 保护

----

## License

```
   Copyright 2021-2022 KasukuSakura & Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```

