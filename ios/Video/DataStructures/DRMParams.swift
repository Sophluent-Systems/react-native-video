public struct DRMParams {
    public let type: String?
    public let licenseServer: String?
    public let headers: [String: Any]?
    public let contentId: String?
    public let certificateUrl: String?
    public let base64Certificate: Bool?
    public let localSourceEncryptionKeyScheme: String?

    let json: NSDictionary?

    init(_ json: NSDictionary!) {
        guard json != nil else {
            self.json = nil
            self.type = nil
            self.licenseServer = nil
            self.contentId = nil
            self.certificateUrl = nil
            self.base64Certificate = nil
            self.headers = nil
            self.localSourceEncryptionKeyScheme = nil
            return
        }
        self.json = json
        self.type = json["type"] as? String
        self.licenseServer = json["licenseServer"] as? String
        self.contentId = json["contentId"] as? String
        self.certificateUrl = json["certificateUrl"] as? String
        self.base64Certificate = json["base64Certificate"] as? Bool
        if let headers = json["headers"] as? [[String: Any]] {
            var _headers: [String: Any] = [:]
            for header in headers {
                if let key = header["key"] as? String, let value = header["value"] {
                    _headers[key] = value
                }
            }
            self.headers = _headers
        } else {
            self.headers = nil
        }
        localSourceEncryptionKeyScheme = json["localSourceEncryptionKeyScheme"] as? String
    }
}
