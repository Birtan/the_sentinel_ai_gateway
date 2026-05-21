[Türkçe](#türkçe) | [English](#english)

---

<a id="türkçe"></a>
# 📐 Sentinel AI Gateway Mimari Dokümantasyonu (TR)

Bu doküman, **Sentinel AI Gateway** projesinde yapılan kritik mimari tercihleri, tasarım kalıplarını ve teknoloji seçimlerinin gerekçelerini derinlemesine açıklar.

## 🛠️ Tasarım Kararları ve Gerekçeleri

### 1. KRaft Modu (Kafka) vs Zookeeper
**Karar:** Dağıtık kuyruk yapısında Apache Kafka, Zookeeper bağımlılığı olmadan **KRaft (Kafka Raft Metadata Mode)** ile konumlandırılmıştır.

**Gerekçeler:**
- **Sadelik ve Kaynak Yönetimi:** Klasik Kafka kurulumlarında metadata yönetimi için ek bir Zookeeper kümesi (cluster) yönetilmesi gerekir. Bu durum hem kaynak tüketimini artırır hem de bakım süreçlerini zorlaştırır. KRaft modunda Kafka broker'ları metadata yönetimini kendi içlerinde gerçekleştirir.
- **Daha Hızlı Kurtarma (Failover) Süresi:** Zookeeper entegrasyonunda, lider seçimi ve metadata senkronizasyonu Zookeeper üzerinden aktığı için kesinti anlarında kurtarma süreleri uzayabilir. KRaft doğrudan Kafka Raft konsensüsü kullandığından lider geçişleri milisaniyeler seviyesindedir.
- **Ölçeklenebilirlik:** KRaft ile Kafka milyarlarca partition'ı daha performanslı yönetebilecek şekilde modernize edilmiştir.

---

### 2. Local LLM (Ollama) vs Cloud LLM (OpenAI, Anthropic vb.)
**Karar:** Yapay zeka katmanı olarak yerel ortamda koşan **Ollama (Llama3)** tercih edilmiştir.

**Gerekçeler:**
- **Veri Gizliliği ve KVKK/GDPR Uyumluluğu:** Sentinel AI Gateway, hassas kullanıcı davranış loglarını ve finansal işlem detaylarını analiz eder. Bulut tabanlı (Cloud) LLM'ler kullanıldığında bu kritik verilerin dış sunuculara (üçüncü taraf API sağlayıcılarına) gönderilmesi gerekir. Yerel LLM kullanımı, tüm veri akışının kurum içi (on-premise) ağ sınırlarında kalmasını güvence altına alır.
- **Sıfır Ağ Maliyeti:** Bulut servislerindeki istek başına ücretlendirme modelinin aksine, yerel LLM kullanımı sınırsız analiz olanağı sunar. Ek olarak, internet erişimindeki dalgalanmalardan veya kesintilerden etkilenmez.
- **Düşük Gecikme (Latency):** Analiz istekleri doğrudan yerel Docker ağı üzerinden yapay zeka konteynerine aktarıldığı için coğrafi ağ gecikmesi sıfıra indirgenmiştir.

---

### 3. Redis Topolojileri ve Rate Limiter Tasarımı
**Karar:** API hız sınırlaması (rate limiting) için Redis üzerinde Token Bucket algoritması uygulanmıştır.

**Gerekçeler:**
- **Neden Token Bucket Algoritması?** Sabit zaman pencereli (fixed-window) algoritmalardaki "sınırda ani istek patlaması" (burst) sorununu önler. İstemcilere hem düzenli bir akış (refill rate) sağlar hem de izin verilen maksimum patlama kapasitesine (bucket capacity) imkan tanır.
- **Üretim Ortamlarındaki Topoloji Seçimleri:**
  - *Yerel POC:* Geliştirme kolaylığı için tekil (Standalone) Redis instance'ı tercih edilmiştir.
  - *Üretim (Production) Ortamı için Öneri:* 
    - **Redis Sentinel:** Yüksek kullanılabilirlik (High Availability) gerektiren, ancak veri boyutunun tek bir sunucu belleğine sığdığı senaryolar için idealdir. Otomatik lider seçimi (failover) sunar.
    - **Redis Cluster:** Veri boyutunun yatayda ölçeklenmesi gerektiği, çok yüksek trafikli senaryolar için tercih edilmelidir (veri sharding yapar). Sentinel AI Gateway hızı için Redis Sentinel topolojisi, veri kaybını önlemek ve yedekliliği sağlamak adına öncelikli tercihtir.

---

### 4. Güvenlik Tasarımı (Keycloak OAuth2 / OIDC)
**Karar:** Kimlik doğrulama ve rol yönetimi için **Keycloak** entegrasyonuyla OAuth2 Resource Server modeli benimsenmiştir.

**Gerekçeler:**
- **Merkezi Kimlik Yönetimi:** Kullanıcı şifreleri, JWT üretimi ve oturum yönetimi tamamen Keycloak'a devredilmiştir (separation of concerns).
- **Rol Tabanlı Yetkilendirme (RBAC):** Keycloak üzerinde tanımlanan roller, özelleştirilmiş bir `JwtAuthenticationConverter` vasıtasıyla Spring Security otoritelerine dönüştürülür. Bu sayede `/analyze` (ROLE_USER, ROLE_ADMIN) ve `/admin/**` (ROLE_ADMIN) gibi uç noktalar güvenli hale getirilmiştir.

---

### 5. Kafka Hata Toleransı & DLT Tasarımı
**Karar:** Kuyruktaki hatalı mesajlar (poison pill) için DLT (Dead Letter Topic) yapısı kurulmuştur.

**Gerekçeler:**
- **Poison Pill Koruması:** JSON deserialize edilemeyen veya işlenirken beklenmeyen hatalar veren mesajlar, tüm kuyruğun bloke olmasına (consumer lag birikmesine) yol açar.
- **Hata Yönetimi (ErrorHandlingDeserializer):** Spring Kafka deserialize hatalarını yakalayarak mesajın işlenmesini kesmeden doğrudan DLT'ye (`sentinel.audit.events.DLT`) yönlendirir. Böylece sağlıklı mesajlar kesintisiz işlenmeye devam eder.

---
---

<a id="english"></a>
# 📐 Sentinel AI Gateway Architecture Documentation (EN)

This document explains in depth the critical architectural choices, design patterns, and rationale behind technology selections in the **Sentinel AI Gateway** project.

## 🛠️ Design Decisions and Rationales

### 1. KRaft Mode (Kafka) vs Zookeeper
**Decision:** In the distributed queue structure, Apache Kafka is positioned with **KRaft (Kafka Raft Metadata Mode)** without Zookeeper dependency.

**Rationales:**
- **Simplicity and Resource Management:** Classic Kafka setups require managing an additional Zookeeper cluster for metadata management. This increases both resource consumption and maintenance complexity. In KRaft mode, Kafka brokers perform metadata management internally.
- **Faster Failover Time:** In Zookeeper integration, metadata synchronization and leader election flow through Zookeeper, which can prolong recovery times during outages. Since KRaft directly uses Kafka Raft consensus, leader transitions occur in milliseconds.
- **Scalability:** With KRaft, Kafka is modernized to manage billions of partitions more performantly.

---

### 2. Local LLM (Ollama) vs Cloud LLM (OpenAI, Anthropic, etc.)
**Decision:** **Ollama (Llama3)** running in the local environment is preferred as the AI layer.

**Rationales:**
- **Data Privacy and GDPR/KVKK Compliance:** Sentinel AI Gateway analyzes sensitive user behavior logs and financial transaction details. When cloud-based (Cloud) LLMs are used, this critical data must be sent to external servers (third-party API providers). Using a local LLM ensures that all data flow remains within the boundaries of the on-premise network.
- **Zero Network Cost:** Unlike the pay-per-request pricing model of cloud services, local LLM usage offers unlimited analysis. Additionally, it is not affected by internet access fluctuations or outages.
- **Low Latency:** Since analysis requests are transferred directly to the AI container over the local Docker network, geographical network latency is reduced to zero.

---

### 3. Redis Topologies and Rate Limiter Design
**Decision:** Token Bucket algorithm is applied on Redis for API rate limiting.

**Rationales:**
- **Why Token Bucket Algorithm?** It prevents the "burst traffic" issue seen in fixed-window algorithms. It provides clients with both a steady flow (refill rate) and allows for a maximum burst capacity (bucket capacity).
- **Topology Selections in Production Environments:**
  - *Local POC:* A single Standalone Redis instance is preferred for ease of development.
  - *Production Environment Recommendations:*
    - **Redis Sentinel:** Ideal for scenarios requiring High Availability but where data fits in the memory of a single server. It offers automatic failover.
    - **Redis Cluster:** Should be preferred for high-traffic scenarios where data size needs to scale horizontally (performs data sharding). For Sentinel AI Gateway performance, Redis Sentinel topology is the primary choice to prevent data loss and provide redundancy.

---

### 4. Security Design (Keycloak OAuth2 / OIDC)
**Decision:** OAuth2 Resource Server model is adopted with **Keycloak** integration for authentication and role management.

**Rationales:**
- **Centralized Identity Management:** User credentials, JWT generation, and session management are completely delegated to Keycloak (separation of concerns).
- **Role-Based Access Control (RBAC):** Roles defined on Keycloak are converted into Spring Security authorities via a custom `JwtAuthenticationConverter`. This secures endpoints like `/analyze` (ROLE_USER, ROLE_ADMIN) and `/admin/**` (ROLE_ADMIN).

---

### 5. Kafka Fault Tolerance & DLT Design
**Decision:** A DLT (Dead Letter Topic) structure has been established for faulty messages (poison pills) in the queue.

**Rationales:**
- **Poison Pill Protection:** Messages that cannot be JSON-deserialized or that throw unexpected errors during processing cause the entire queue to block (consumer lag accumulation).
- **Error Management (ErrorHandlingDeserializer):** Spring Kafka catches deserialization errors and routes the message directly to the DLT (`sentinel.audit.events.DLT`) without interrupting the processing of other messages. Thus, healthy messages continue to be processed without disruption.
