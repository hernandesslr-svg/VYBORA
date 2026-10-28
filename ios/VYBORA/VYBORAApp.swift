import SwiftUI
import Foundation
import Security

private enum VYBORAConfig {
    static let baseURL: String = {
        let configured = (Bundle.main.object(forInfoDictionaryKey: "VYBORA_API_BASE_URL") as? String ?? "")
            .trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        return configured.isEmpty ? "https://vybora-production.up.railway.app" : configured
    }()
}

private enum SessionKeychain {
    private static let service = "app.vybora.ios"
    private static let account = "session-token"

    static func read() -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var result: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &result) == errSecSuccess,
              let data = result as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }

    static func save(_ token: String) {
        let data = Data(token.utf8)
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
        SecItemDelete(query as CFDictionary)
        var attributes = query
        attributes[kSecValueData as String] = data
        attributes[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        SecItemAdd(attributes as CFDictionary, nil)
    }

    static func clear() {
        SecItemDelete([
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ] as CFDictionary)
    }
}

private enum VYBORAAPI {
    static func request(_ path: String, method: String = "GET", body: [String: Any]? = nil, token: String? = nil) async throws -> Any {
        guard let url = URL(string: VYBORAConfig.baseURL + path) else {
            throw APIError.message("Endereço da API inválido.")
        }
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.timeoutInterval = 20
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if let body {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = try JSONSerialization.data(withJSONObject: body)
        }
        if let token, !token.isEmpty {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        let (data, response): (Data, URLResponse)
        do {
            (data, response) = try await URLSession.shared.data(for: request)
        } catch {
            throw APIError.message("Não foi possível conectar à API VYBORA. Confira a publicação do backend e tente novamente.")
        }
        guard let http = response as? HTTPURLResponse else {
            throw APIError.message("Resposta inválida do servidor.")
        }
        let json = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] ?? [:]
        guard (200..<300).contains(http.statusCode) else {
            throw APIError.message((json["error"] as? String) ?? "A API respondeu com HTTP \(http.statusCode).")
        }
        return json
    }

    static func authenticate(register: Bool, username: String, email: String, password: String) async throws -> (String, String) {
        var body: [String: Any] = ["email": email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(), "password": password]
        if register { body["username"] = username.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() }
        let path = register ? "/api/auth/register" : "/api/auth/login"
        let response = try await request(path, method: "POST", body: body) as? [String: Any]
        guard let response, let token = response["token"] as? String,
              let user = response["user"] as? [String: Any],
              let username = user["username"] as? String, !token.isEmpty else {
            throw APIError.message("A API não retornou uma sessão válida.")
        }
        return (token, username)
    }

    static func posts() async throws -> [FeedPost] {
        let response = try await request("/api/posts?limit=30") as? [String: Any]
        let raw = response?["posts"] as? [[String: Any]] ?? []
        return raw.map { item in
            let author = item["author"] as? [String: Any] ?? [:]
            return FeedPost(
                id: item["id"] as? String ?? UUID().uuidString,
                caption: item["caption"] as? String ?? "",
                mediaURL: item["mediaUrl"] as? String ?? "",
                mediaType: item["mediaType"] as? String ?? "text",
                username: author["username"] as? String ?? "usuario",
                displayName: author["displayName"] as? String ?? author["display_name"] as? String ?? ""
            )
        }
    }
}

private enum APIError: LocalizedError {
    case message(String)
    var errorDescription: String? {
        if case .message(let value) = self { return value }
        return "Ocorreu um erro inesperado."
    }
}

private struct FeedPost: Identifiable {
    let id: String
    let caption: String
    let mediaURL: String
    let mediaType: String
    let username: String
    let displayName: String
}

@main
struct VYBORAApp: App {
    var body: some Scene {
        WindowGroup {
            VYBORAHome()
                .preferredColorScheme(.dark)
                .tint(Color(red: 1, green: 0.22, blue: 0.58))
        }
    }
}

private struct VYBORAHome: View {
    @State private var token: String? = SessionKeychain.read()
    @State private var username = ""
    @State private var posts: [FeedPost] = []
    @State private var isLoading = false
    @State private var feedError: String?
    @State private var selectedTab = 0

    var body: some View {
        Group {
            if let token {
                TabView(selection: $selectedTab) {
                    feed(token: token)
                        .tabItem { Label("Início", systemImage: "house.fill") }
                        .tag(0)
                    placeholder(title: "Buscar", detail: "A busca de pessoas será ligada à API na próxima etapa.", icon: "magnifyingglass")
                        .tabItem { Label("Buscar", systemImage: "magnifyingglass") }
                        .tag(1)
                    placeholder(title: "Publicar", detail: "A publicação de fotos e vídeos será integrada na próxima etapa.", icon: "plus.app")
                        .tabItem { Label("Publicar", systemImage: "plus.app") }
                        .tag(2)
                    placeholder(title: "Mensagens", detail: "A tela de conversas será integrada aos endpoints existentes.", icon: "bubble.left.and.bubble.right")
                        .tabItem { Label("Mensagens", systemImage: "bubble.left.and.bubble.right") }
                        .tag(3)
                    profile
                        .tabItem { Label("Perfil", systemImage: "person.crop.circle") }
                        .tag(4)
                }
            } else {
                LoginView { newToken, newUsername in
                    SessionKeychain.save(newToken)
                    username = newUsername
                    token = newToken
                    selectedTab = 0
                }
            }
        }
        .task {
            if let token { await loadFeed(token: token) }
        }
    }

    private func feed(token: String) -> some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: 18) {
                    if let feedError {
                        ContentUnavailableView("Feed indisponível", systemImage: "wifi.exclamationmark", description: Text(feedError))
                            .padding(.top, 28)
                    } else if posts.isEmpty && !isLoading {
                        ContentUnavailableView("Ainda sem publicações", systemImage: "play.rectangle", description: Text("Quando a API retornar publicações, elas aparecerão aqui."))
                            .padding(.top, 28)
                    }
                    ForEach(posts) { post in
                        FeedPostCard(post: post)
                    }
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
            }
            .background(Color.black)
            .navigationTitle("VYBORA")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { Task { await loadFeed(token: token) } } label: {
                        Image(systemName: "arrow.clockwise")
                    }
                    .accessibilityLabel("Atualizar feed")
                }
            }
            .overlay {
                if isLoading { ProgressView("Carregando…").padding().background(.ultraThinMaterial).clipShape(RoundedRectangle(cornerRadius: 14)) }
            }
        }
    }

    private var profile: some View {
        NavigationStack {
            VStack(spacing: 16) {
                Image(systemName: "person.crop.circle.fill")
                    .font(.system(size: 76))
                    .foregroundStyle(Color.pink)
                Text(username.isEmpty ? "Conta conectada" : "@\(username)")
                    .font(.title3.bold())
                Text("Versão iOS de testes do VYBORA")
                    .foregroundStyle(.secondary)
                Button(role: .destructive) {
                    SessionKeychain.clear()
                    token = nil
                    username = ""
                    posts = []
                } label: {
                    Label("Sair da conta", systemImage: "rectangle.portrait.and.arrow.right")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .padding(.horizontal, 28)
                Spacer()
            }
            .padding(.top, 40)
            .frame(maxWidth: .infinity)
            .background(Color.black)
            .navigationTitle("Perfil")
        }
    }

    private func placeholder(title: String, detail: String, icon: String) -> some View {
        NavigationStack {
            ContentUnavailableView(title, systemImage: icon, description: Text(detail))
                .navigationTitle(title)
                .background(Color.black)
        }
    }

    @MainActor
    private func loadFeed(token: String) async {
        isLoading = true
        feedError = nil
        defer { isLoading = false }
        do {
            posts = try await VYBORAAPI.posts()
        } catch {
            feedError = error.localizedDescription
        }
    }
}

private struct FeedPostCard: View {
    let post: FeedPost

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 10) {
                Circle()
                    .fill(LinearGradient(colors: [.pink, .purple], startPoint: .topLeading, endPoint: .bottomTrailing))
                    .frame(width: 42, height: 42)
                    .overlay(Text(String(post.username.prefix(1)).uppercased()).font(.headline).foregroundStyle(.white))
                VStack(alignment: .leading, spacing: 2) {
                    Text(post.displayName.isEmpty ? post.username : post.displayName).font(.subheadline.bold())
                    Text("@\(post.username)").font(.caption).foregroundStyle(.secondary)
                }
                Spacer()
                Image(systemName: "ellipsis").foregroundStyle(.secondary)
            }

            if post.mediaType.lowercased().contains("image"), let url = URL(string: post.mediaURL), !post.mediaURL.isEmpty {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image): image.resizable().scaledToFill()
                    case .failure: mediaFallback
                    case .empty: ProgressView().frame(maxWidth: .infinity).frame(height: 260)
                    @unknown default: mediaFallback
                    }
                }
                .frame(maxWidth: .infinity)
                .frame(height: 390)
                .clipped()
                .clipShape(RoundedRectangle(cornerRadius: 14))
            } else if post.mediaType.lowercased().contains("video") {
                ZStack {
                    RoundedRectangle(cornerRadius: 14).fill(Color(white: 0.1))
                    VStack(spacing: 10) {
                        Image(systemName: "play.circle.fill").font(.system(size: 52))
                        Text("Vídeo — reprodução será integrada nesta etapa")
                            .font(.caption).foregroundStyle(.secondary)
                    }
                }
                .frame(height: 260)
            }

            if !post.caption.isEmpty {
                Text(post.caption).font(.body).frame(maxWidth: .infinity, alignment: .leading)
            }
            HStack(spacing: 18) {
                Label("Curtir", systemImage: "heart")
                Label("Comentar", systemImage: "bubble.right")
                Spacer()
                Image(systemName: "square.and.arrow.up")
            }
            .font(.subheadline)
            .foregroundStyle(.secondary)
            .padding(.top, 2)
        }
        .padding(12)
        .background(Color(white: 0.07))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }

    private var mediaFallback: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 14).fill(Color(white: 0.12))
            Image(systemName: "photo").font(.largeTitle).foregroundStyle(.secondary)
        }
        .frame(height: 260)
    }
}

private struct LoginView: View {
    let onLogin: (String, String) -> Void
    @State private var registerMode = false
    @State private var username = ""
    @State private var email = ""
    @State private var password = ""
    @State private var isLoading = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    Spacer(minLength: 28)
                    ZStack {
                        RoundedRectangle(cornerRadius: 28)
                            .fill(LinearGradient(colors: [.pink, .purple], startPoint: .topLeading, endPoint: .bottomTrailing))
                            .frame(width: 88, height: 88)
                        Image(systemName: "sparkles").font(.system(size: 38, weight: .bold)).foregroundStyle(.white)
                    }
                    Text("VYBORA").font(.system(size: 32, weight: .black, design: .rounded))
                    Text(registerMode ? "Crie sua conta para começar" : "Entre para continuar")
                        .foregroundStyle(.secondary)

                    VStack(spacing: 12) {
                        if registerMode {
                            TextField("Nome de usuário", text: $username)
                                .textContentType(.username)
                                .textInputAutocapitalization(.never)
                                .autocorrectionDisabled()
                                .textFieldStyle(.roundedBorder)
                        }
                        TextField("E-mail", text: $email)
                            .keyboardType(.emailAddress)
                            .textContentType(.emailAddress)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .textFieldStyle(.roundedBorder)
                        SecureField("Senha", text: $password)
                            .textContentType(registerMode ? .newPassword : .password)
                            .textFieldStyle(.roundedBorder)
                    }
                    .padding(.horizontal, 26)

                    if let errorMessage {
                        Text(errorMessage).font(.footnote).foregroundStyle(.red).padding(.horizontal, 26)
                    }

                    Button {
                        Task { await submit() }
                    } label: {
                        HStack {
                            if isLoading { ProgressView().tint(.white) }
                            Text(registerMode ? "Criar conta" : "Entrar")
                                .fontWeight(.bold)
                        }
                        .frame(maxWidth: .infinity).padding(.vertical, 14)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(Color(red: 1, green: 0.22, blue: 0.58))
                    .disabled(isLoading || email.isEmpty || password.isEmpty || (registerMode && username.isEmpty))
                    .padding(.horizontal, 26)

                    Button(registerMode ? "Já tenho conta — entrar" : "Não tenho conta — cadastrar") {
                        registerMode.toggle()
                        errorMessage = nil
                    }
                    .font(.subheadline)
                    Spacer(minLength: 24)
                    Text("Versão iOS inicial para testes. Usa a API VYBORA configurada no projeto.")
                        .font(.caption2).foregroundStyle(.tertiary).multilineTextAlignment(.center)
                        .padding(.horizontal, 28)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 20)
            }
            .background(Color.black.ignoresSafeArea())
            .toolbar(.hidden, for: .navigationBar)
        }
    }

    @MainActor
    private func submit() async {
        errorMessage = nil
        isLoading = true
        defer { isLoading = false }
        do {
            let (token, username) = try await VYBORAAPI.authenticate(
                register: registerMode,
                username: username,
                email: email,
                password: password
            )
            onLogin(token, username)
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
